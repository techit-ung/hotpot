package com.coloncmd.hotpot.server

import com.coloncmd.hotpot.dsl.HandlerContext
import com.coloncmd.hotpot.dsl.RouteDefinition
import com.coloncmd.hotpot.dsl.RouteGroup
import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.model.WebhookResponse
import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.notification.NotifyDefinition
import com.coloncmd.hotpot.openapi.HotPotOpenApi
import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeEmptyResponse
import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeJsonRequest
import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeJsonResponse
import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeSignatureHeaders
import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeTokenAuth
import com.coloncmd.hotpot.openapi.HotPotOpenApi.hasTokenAuth
import com.coloncmd.hotpot.plugin.validateAuth
import com.coloncmd.hotpot.plugin.validateSignature
import com.coloncmd.hotpot.routing.QueryRouter
import com.coloncmd.hotpot.storage.Storage
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.Operation.Builder
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class HotPotServer(private val scope: StartScope, private val port: Int = 8080) {

    fun start(wait: Boolean = true) {
        embeddedServer(CIO, port = port) {
            configureApplication(this, scope)
        }.start(wait = wait)
    }

    companion object {
        fun configureApplication(app: Application, scope: StartScope) = app.apply {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
            }
            val queryRoutes = QueryRouter.install(this, scope.storage)
            val webhookRoutes = installWebhookRoutes(scope)
            HotPotOpenApi.install(this, listOf(queryRoutes, webhookRoutes))
        }

        private fun Application.installWebhookRoutes(scope: StartScope): Route = routing {
            for (group in scope.routeGroups) {
                route(group.basePath) {
                    for (routeDef in group.routes) {
                        mountRoute(routeDef, group, scope.storage)
                    }
                    for (notifyDef in group.notifyRoutes) {
                        mountNotify(notifyDef, group, scope.notificationService)
                    }
                }
            }
        }

        private fun Route.mountRoute(routeDef: RouteDefinition, group: RouteGroup, storage: Storage) {
            val effectiveSave = routeDef.saveRequestResponse ?: group.saveRequestResponse

            suspend fun io.ktor.server.routing.RoutingContext.handle() {
                val rawBody = call.receiveChannel().toByteArray()
                if (!call.validateAuth(routeDef.auth)) return
                if (!call.validateSignature(routeDef.signature, rawBody)) return

                val request = WebhookRequest(
                    id = "",
                    path = call.request.path(),
                    method = call.request.httpMethod.value,
                    headers = call.request.headers.entries().associate { it.key to it.value },
                    body = rawBody.decodeToString(),
                    receivedAt = Clock.System.now(),
                )
                val requestId = if (effectiveSave) storage.saveRequest(request) else null
                val response = routeDef.handler.invoke(HandlerContext(call, storage), request)
                if (effectiveSave && requestId != null) {
                    storage.saveResponse(
                        requestId,
                        WebhookResponse(
                            requestId = requestId,
                            status = response.status,
                            body = response.body.toString(),
                            sentAt = Clock.System.now(),
                        )
                    )
                }
                call.respond(HttpStatusCode.fromValue(response.status), response.body)
            }

            when (routeDef) {
                is RouteDefinition.Post -> post(routeDef.path) { handle() }.describe {
                    describeInboundRoute(routeDef, group)
                }
                is RouteDefinition.Get -> get(routeDef.path) { handle() }.describe {
                    describeInboundRoute(routeDef, group)
                }
            }
        }

        private fun Route.mountNotify(
            notify: NotifyDefinition,
            group: RouteGroup,
            notificationService: NotificationService,
        ) {
            post(notify.path) {
                val rawBody = call.receiveChannel().toByteArray()
                val request = WebhookRequest(
                    id = "",
                    path = call.request.path(),
                    method = "POST",
                    headers = call.request.headers.entries().associate { it.key to it.value },
                    body = rawBody.decodeToString(),
                    receivedAt = Clock.System.now(),
                )
                notificationService.dispatch(notify, request)
                call.respond(HttpStatusCode.Accepted)
            }.describe {
                summary = "POST ${fullRoutePath(group.basePath, notify.path)}"
                description = "Receives an inbound payload and dispatches configured outgoing notification logic."
                operationId = operationId("notify", notify.path)
                tag(tagName(group.basePath))
                describeJsonRequest()
                describeEmptyResponse(HttpStatusCode.Accepted, "Notification accepted for dispatch.")
            }
        }

        private fun Builder.describeInboundRoute(
            routeDef: RouteDefinition,
            group: RouteGroup,
        ) {
            val method = when (routeDef) {
                is RouteDefinition.Get -> "GET"
                is RouteDefinition.Post -> "POST"
            }

            summary = "$method ${fullRoutePath(group.basePath, routeDef.path)}"
            description = "HotPot DSL route."
            operationId = operationId(method.lowercase(), routeDef.path)
            tag(tagName(group.basePath))

            if (routeDef is RouteDefinition.Post) {
                describeJsonRequest()
            }

            if (routeDef.auth.hasTokenAuth()) {
                describeTokenAuth()
            }
            describeSignatureHeaders(routeDef.signature)

            describeJsonResponse(HttpStatusCode.OK, "Generic JSON response payload.")
            if (routeDef is RouteDefinition.Post) {
                describeJsonResponse(HttpStatusCode.Accepted, "Generic JSON response payload.")
            }
            describeEmptyResponse(HttpStatusCode.BadRequest, "Invalid request or signature.")
            describeEmptyResponse(HttpStatusCode.Unauthorized, "Authentication failed.")
        }

        private fun fullRoutePath(basePath: String, path: String): String {
            val normalizedBase = if (basePath.isBlank()) "" else basePath.trimEnd('/')
            val normalizedPath = if (path.isBlank()) "" else if (path.startsWith("/")) path else "/$path"
            return (normalizedBase + normalizedPath).ifBlank { "/" }
        }

        private fun operationId(prefix: String, path: String): String =
            (prefix + "_" + path.trim('/').replace('/', '_').ifBlank { "root" })
                .replace(Regex("[^A-Za-z0-9_]"), "_")

        private fun tagName(basePath: String): String =
            basePath.trim('/').ifBlank { "root" }
    }
}

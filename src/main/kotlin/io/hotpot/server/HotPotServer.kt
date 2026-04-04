package io.hotpot.server

import io.hotpot.dsl.HandlerContext
import io.hotpot.dsl.RouteDefinition
import io.hotpot.dsl.RouteGroup
import io.hotpot.dsl.StartScope
import io.hotpot.model.WebhookRequest
import io.hotpot.model.WebhookResponse
import io.hotpot.notification.NotificationService
import io.hotpot.notification.NotifyDefinition
import io.hotpot.plugin.validateAuth
import io.hotpot.plugin.validateSignature
import io.hotpot.routing.QueryRouter
import io.hotpot.storage.Storage
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json

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
            QueryRouter.install(this, scope.storage)
            installWebhookRoutes(scope)
        }

        private fun Application.installWebhookRoutes(scope: StartScope) = routing {
            for (group in scope.routeGroups) {
                route(group.basePath) {
                    for (routeDef in group.routes) mountRoute(routeDef, group, scope.storage)
                    for (notifyDef in group.notifyRoutes) mountNotify(notifyDef, scope.notificationService)
                }
            }
        }

        private fun Route.mountRoute(routeDef: RouteDefinition, group: RouteGroup, storage: Storage) {
            val effectiveSave = routeDef.saveRequestResponse ?: group.saveRequestResponse

            suspend fun RoutingContext.handle() {
                val rawBody = call.receiveChannel().toByteArray()
                if (!call.validateAuth(routeDef.auth)) return
                if (!call.validateSignature(routeDef.signature, rawBody)) return

                val request = WebhookRequest(
                    id = "",
                    path = call.request.path(),
                    method = call.request.httpMethod.value,
                    headers = call.request.headers.entries().associate { it.key to it.value },
                    body = rawBody.decodeToString(),
                    receivedAt = System.currentTimeMillis(),
                )
                val requestId = if (effectiveSave) storage.saveRequest(request) else null
                val response = routeDef.handler.invoke(HandlerContext(call, storage), request)
                if (effectiveSave && requestId != null) {
                    storage.saveResponse(requestId, WebhookResponse(
                        requestId = requestId,
                        status = response.status,
                        body = response.body.toString(),
                        sentAt = System.currentTimeMillis(),
                    ))
                }
                call.respond(HttpStatusCode.fromValue(response.status), response.body)
            }

            when (routeDef) {
                is RouteDefinition.Post -> post(routeDef.path) { handle() }
                is RouteDefinition.Get  -> get(routeDef.path)  { handle() }
            }
        }

        private fun Route.mountNotify(notify: NotifyDefinition, notificationService: NotificationService) =
            post(notify.path) {
                val rawBody = call.receiveChannel().toByteArray()
                val request = WebhookRequest(
                    id = "",
                    path = call.request.path(),
                    method = "POST",
                    headers = call.request.headers.entries().associate { it.key to it.value },
                    body = rawBody.decodeToString(),
                    receivedAt = System.currentTimeMillis(),
                )
                notificationService.dispatch(notify, request)
                call.respond(HttpStatusCode.Accepted)
            }
    }
}

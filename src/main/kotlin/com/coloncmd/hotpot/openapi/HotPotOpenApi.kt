package com.coloncmd.hotpot.openapi

import com.coloncmd.hotpot.auth.AuthStrategy
import com.coloncmd.hotpot.auth.TokenAuthentication
import com.coloncmd.hotpot.signature.HMACSignatureValidation
import com.coloncmd.hotpot.signature.SignatureStrategy
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.HttpSecurityScheme
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.openapi.Operation
import io.ktor.openapi.ReferenceOr
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.typeOf

internal object HotPotOpenApi {
    const val SPEC_PATH = "/hotpot/openapi.json"
    private const val BEARER_AUTH_SCHEME = "bearerAuth"

    fun install(
        app: Application,
        documentedRoots: List<Route>,
    ) {
        val source =
            OpenApiDocSource.Routing(
                contentType = ContentType.Application.Json,
                securitySchemes = {
                    mapOf(
                        BEARER_AUTH_SCHEME to
                            ReferenceOr.Value(
                                HttpSecurityScheme(
                                    scheme = "bearer",
                                    bearerFormat = "JWT",
                                    description = "Bearer token used by TokenAuthentication.",
                                ),
                            ),
                    )
                },
                routes = { documentedRoots.asSequence().flatMap { it.allDescendants() } },
            )
        app.routing {
            route("/hotpot") {
                get("openapi.json") {
                    val doc =
                        source.read(
                            call.application,
                            OpenApiDoc(info = OpenApiInfo(title = "HotPot", version = app.hotPotVersion())),
                        )
                    call.respondText(doc.content, doc.contentType)
                }.hide()
                swaggerUI("swagger", SPEC_PATH).hide()
            }
        }
    }

    fun Route.allDescendants(): Sequence<Route> = sequenceOf(this) + children.asSequence().flatMap { it.allDescendants() }

    @OptIn(ExperimentalStdlibApi::class)
    fun Operation.Builder.describeJsonRequest(required: Boolean = true) {
        requestBody {
            description = "Generic JSON request payload."
            this.required = required
            schema = buildSchema(typeOf<JsonElement>())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Operation.Builder.describeJsonResponse(
        status: HttpStatusCode,
        description: String,
    ) {
        responses {
            status {
                this.description = description
                schema = buildSchema(typeOf<JsonElement>())
            }
        }
    }

    fun Operation.Builder.describeEmptyResponse(
        status: HttpStatusCode,
        description: String,
    ) {
        responses {
            status {
                this.description = description
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Operation.Builder.describeTokenAuth() {
        security {
            requirement(BEARER_AUTH_SCHEME, emptyList())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Operation.Builder.describeSignatureHeaders(signatures: Set<SignatureStrategy>) {
        signatures
            .filterIsInstance<HMACSignatureValidation>()
            .map { it.headerName }
            .distinct()
            .forEach { headerName ->
                parameters {
                    header(headerName) {
                        required = true
                        description = "HMAC signature header."
                        schema = buildSchema(typeOf<String>())
                    }
                }
            }
    }

    fun Set<AuthStrategy>.hasTokenAuth(): Boolean = any { it is TokenAuthentication }

    private fun Application.hotPotVersion(): String = this::class.java.`package`?.implementationVersion ?: "0.1.0-SNAPSHOT"
}

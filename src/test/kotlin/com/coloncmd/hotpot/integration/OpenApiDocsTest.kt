package com.coloncmd.hotpot.integration

import com.coloncmd.hotpot.auth.TokenAuthentication
import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.signature.HMACSignatureValidation
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

class OpenApiDocsTest :
    FunSpec({

        fun appScope(): StartScope =
            StartScope(InMemoryStorage()).apply {
                listen(path = "/payments") {
                    post(
                        path = "",
                        auth = setOf(TokenAuthentication("secret")),
                        signature = setOf(HMACSignatureValidation("my-secret")),
                    ) { _ ->
                        HotPotResponse.accepted()
                    }

                    get(path = "/health") { _ ->
                        HotPotResponse.ok()
                    }

                    notify(path = "/callbacks", target = "http://service/callbacks")
                }
            }

        test("OpenAPI spec endpoint returns generated JSON") {
            testApplication {
                application { HotPotServer.configureApplication(this, appScope()) }

                val response = client.get("/hotpot/openapi.json")
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.ContentType] shouldContain ContentType.Application.Json.toString()

                val body = response.bodyAsText()
                body shouldContain "\"openapi\""
                body shouldContain "\"/payments\""
                body shouldContain "\"/hotpot/requests\""
            }
        }

        test("OpenAPI spec excludes docs routes and includes documented headers") {
        testApplication {
            application { HotPotServer.configureApplication(this, appScope()) }

            val body = client.get("/hotpot/openapi.json").bodyAsText()
            body shouldContain "\"bearerAuth\""
            body shouldContain "\"security\":[{\"bearerAuth\":[]}]"
            body shouldContain "\"X-Hub-Signature-256\""
            body shouldContain "\"/payments/callbacks\""
            body shouldNotContain "\"/hotpot/openapi.json\""
                body shouldNotContain "\"/hotpot/swagger\""
            }
        }

        test("Swagger UI endpoint is served") {
            testApplication {
                application { HotPotServer.configureApplication(this, appScope()) }

                val response = client.get("/hotpot/swagger")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Swagger UI"
            }
        }
    })

package com.coloncmd.hotpot.standalone.integration

import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.standalone.config.AuthConfig
import com.coloncmd.hotpot.standalone.config.ConfigBridge
import com.coloncmd.hotpot.standalone.config.HotPotConfig
import com.coloncmd.hotpot.standalone.config.ResponseConfig
import com.coloncmd.hotpot.standalone.config.RouteConfig
import com.coloncmd.hotpot.standalone.config.RouteGroupConfig
import com.coloncmd.hotpot.standalone.config.SignatureConfig
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class StandaloneIntegrationTest :
    FunSpec({
        val apiToken = "test-token"
        val hmacSecret = "test-secret" // pragma: allowlist secret

        fun sign(
            body: String,
            secret: String,
        ): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            val hash = mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
            return "sha256=$hash"
        }

        fun config(
            responseBody: String = "{}",
            responseStatus: Int = 202,
            method: String = "POST",
            auth: List<AuthConfig> = emptyList(),
            signature: List<SignatureConfig> = emptyList(),
        ) = HotPotConfig(
            groups =
                listOf(
                    RouteGroupConfig(
                        path = "/payments",
                        routes =
                            listOf(
                                RouteConfig(
                                    method = method,
                                    path = "",
                                    auth = auth,
                                    signature = signature,
                                    response = ResponseConfig(status = responseStatus, body = responseBody),
                                ),
                            ),
                    ),
                ),
        )

        fun scope(config: HotPotConfig) = ConfigBridge.toStartScope(config, InMemoryStorage())

        test("POST to configured route returns configured status and body") {
            // arrange
            val scope = scope(config(responseBody = """{"message": "ok"}""", responseStatus = 202))

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val response =
                    client.post("/payments") {
                        setBody("{}")
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.Accepted
                response.bodyAsText() shouldContain "ok"
            }
        }

        test("response body renders request.id template variable") {
            // arrange
            val scope = scope(config(responseBody = """{"id": "{{request.id}}"}"""))

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val body =
                    client
                        .post("/payments") {
                            setBody("{}")
                            contentType(ContentType.Application.Json)
                        }.bodyAsText()

                body shouldContain """"id":"""
                // id should be a non-empty string (UUID assigned by server)
                val id = body.substringAfter(""""id": """").substringBefore('"')
                id.shouldNotBeBlank()
            }
        }

        test("response body renders request.body.field template variable") {
            // arrange
            val scope = scope(config(responseBody = """{"amount": "{{request.body.amount}}"}"""))

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val body =
                    client
                        .post("/payments") {
                            setBody("""{"amount": "99.00"}""")
                            contentType(ContentType.Application.Json)
                        }.bodyAsText()

                body shouldContain "99.00"
            }
        }

        test("GET route returns configured body") {
            // arrange
            val scope = scope(config(method = "GET", responseBody = """{"status": "ok"}""", responseStatus = 200))

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val response = client.get("/payments")

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "ok"
            }
        }

        test("returns 401 when Bearer token is wrong") {
            // arrange
            val scope = scope(config(auth = listOf(AuthConfig.Token(apiToken))))

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val response =
                    client.post("/payments") {
                        bearerAuth("wrong-token")
                        setBody("{}")
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("returns 400 when HMAC signature is wrong") {
            // arrange
            val scope =
                scope(
                    config(
                        auth = listOf(AuthConfig.Token(apiToken)),
                        signature = listOf(SignatureConfig.Hmac(secret = hmacSecret)),
                    ),
                )

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val response =
                    client.post("/payments") {
                        bearerAuth(apiToken)
                        headers.append("X-Hub-Signature-256", "sha256=badhash")
                        setBody("{}")
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("accepts request with correct token and HMAC signature") {
            // arrange
            val scope =
                scope(
                    config(
                        auth = listOf(AuthConfig.Token(apiToken)),
                        signature = listOf(SignatureConfig.Hmac(secret = hmacSecret)),
                        responseBody = """{"ok": true}""",
                    ),
                )
            val body = """{"amount": "50.00"}"""

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }

                val response =
                    client.post("/payments") {
                        bearerAuth(apiToken)
                        headers.append("X-Hub-Signature-256", sign(body, hmacSecret))
                        setBody(body)
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.Accepted
                response.bodyAsText() shouldContain "true"
            }
        }
    })

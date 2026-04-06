package com.coloncmd.hotpot.standalone.config

import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

class ConfigBridgeTest :
    FunSpec({
        fun scope(config: HotPotConfig) = ConfigBridge.toStartScope(config, InMemoryStorage())

        fun sign(
            body: String,
            secret: String,
        ): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            val hash = mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
            return "sha256=$hash"
        }

        test("POST route responds with configured status and body") {
            // arrange
            val config =
                HotPotConfig(
                    groups =
                        listOf(
                            RouteGroupConfig(
                                path = "/orders",
                                routes =
                                    listOf(
                                        RouteConfig(
                                            method = "POST",
                                            path = "",
                                            response = ResponseConfig(status = 201, body = """{"created": true}"""),
                                        ),
                                    ),
                            ),
                        ),
                )

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope(config)) }

                val response =
                    client.post("/orders") {
                        setBody("{}")
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.Created
                response.bodyAsText() shouldContain "true"
            }
        }

        test("GET route responds with configured body") {
            // arrange
            val config =
                HotPotConfig(
                    groups =
                        listOf(
                            RouteGroupConfig(
                                path = "/health",
                                routes =
                                    listOf(
                                        RouteConfig(
                                            method = "GET",
                                            path = "",
                                            response = ResponseConfig(status = 200, body = """{"ok": true}"""),
                                        ),
                                    ),
                            ),
                        ),
                )

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope(config)) }

                val response = client.get("/health")

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "true"
            }
        }

        test("TokenAuthentication rejects wrong bearer token") {
            // arrange
            val config =
                HotPotConfig(
                    groups =
                        listOf(
                            RouteGroupConfig(
                                routes =
                                    listOf(
                                        RouteConfig(
                                            method = "POST",
                                            auth = listOf(AuthConfig.Token("correct-token")),
                                        ),
                                    ),
                            ),
                        ),
                )

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope(config)) }

                val response =
                    client.post("/") {
                        bearerAuth("wrong-token")
                        setBody("{}")
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("HMACSignatureValidation rejects wrong signature") {
            // arrange
            val config =
                HotPotConfig(
                    groups =
                        listOf(
                            RouteGroupConfig(
                                routes =
                                    listOf(
                                        RouteConfig(
                                            method = "POST",
                                            signature =
                                                listOf(
                                                    SignatureConfig.Hmac(
                                                        secret = "real-secret", // pragma: allowlist secret
                                                        headerName = "X-Hub-Signature-256",
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                )

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope(config)) }

                val response =
                    client.post("/") {
                        headers.append("X-Hub-Signature-256", "sha256=badhash")
                        setBody("{}")
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("HMACSignatureValidation accepts correct signature") {
            // arrange
            val secret = "real-secret" // pragma: allowlist secret
            val config =
                HotPotConfig(
                    groups =
                        listOf(
                            RouteGroupConfig(
                                routes =
                                    listOf(
                                        RouteConfig(
                                            method = "POST",
                                            signature = listOf(SignatureConfig.Hmac(secret = secret)),
                                            response = ResponseConfig(status = 202, body = "{}"),
                                        ),
                                    ),
                            ),
                        ),
                )
            val body = """{"event": "test"}"""

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope(config)) }

                val response =
                    client.post("/") {
                        headers.append("X-Hub-Signature-256", sign(body, secret))
                        setBody(body)
                        contentType(ContentType.Application.Json)
                    }

                response.status shouldBe HttpStatusCode.Accepted
            }
        }

        test("multiple groups are all mounted") {
            // arrange
            val config =
                HotPotConfig(
                    groups =
                        listOf(
                            RouteGroupConfig(
                                path = "/a",
                                routes =
                                    listOf(
                                        RouteConfig(method = "POST", response = ResponseConfig(status = 200, body = """{"group": "a"}""")),
                                    ),
                            ),
                            RouteGroupConfig(
                                path = "/b",
                                routes =
                                    listOf(
                                        RouteConfig(method = "POST", response = ResponseConfig(status = 200, body = """{"group": "b"}""")),
                                    ),
                            ),
                        ),
                )

            // act / assert
            testApplication {
                application { HotPotServer.configureApplication(this, scope(config)) }

                val a =
                    client
                        .post("/a") {
                            setBody("{}")
                            contentType(ContentType.Application.Json)
                        }.bodyAsText()
                val b =
                    client
                        .post("/b") {
                            setBody("{}")
                            contentType(ContentType.Application.Json)
                        }.bodyAsText()

                a shouldContain """"a""""
                b shouldContain """"b""""
            }
        }
    })

package com.coloncmd.hotpot.integration

import com.coloncmd.hotpot.auth.TokenAuthentication
import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.signature.HMACSignatureValidation
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookEndpointTest :
    FunSpec({

        fun startScope(
            storage: InMemoryStorage = InMemoryStorage(),
            block: StartScope.() -> Unit,
        ): StartScope = StartScope(storage).apply(block)

        test("POST to a registered route returns DSL-defined response") {
            // arrange
            val scope =
                startScope {
                    listen(path = "/paymob") {
                        post(path = "/callback") { _ ->
                            HotPotResponse.ok(buildJsonObject { put("status", "received") })
                        }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                val response =
                    client
                    .post("/paymob/callback") {
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("request is saved to storage when saveRequestResponse = true") {
            // arrange
            val storage = InMemoryStorage()
            val scope =
                startScope(storage) {
                    listen(path = "/paymob", saveRequestResponse = true) {
                        post(path = "/callback") { _ -> HotPotResponse.ok() }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                client.post("/paymob/callback") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"hello":"world"}""")
                }
            }

            // assert
            val savedRequests = storage.findRequests()
            savedRequests shouldBe listOf(savedRequests.first())
            savedRequests.first().path shouldBe "/paymob/callback"
        }

        test("request is NOT saved when saveRequestResponse = false") {
            // arrange
            val storage = InMemoryStorage()
            val scope =
                startScope(storage) {
                    listen(path = "/paymob", saveRequestResponse = false) {
                        post(path = "/callback") { _ -> HotPotResponse.ok() }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                client.post("/paymob/callback") { setBody("{}") }
            }

            // assert
            storage.findRequests().size shouldBe 0
        }

        test("per-route saveRequestResponse overrides listen-level setting") {
            // arrange
            val storage = InMemoryStorage()
            val scope =
                startScope(storage) {
                    listen(path = "/p", saveRequestResponse = false) {
                        post(path = "/save-me", saveRequestResponse = true) { _ -> HotPotResponse.ok() }
                        post(path = "/skip-me") { _ -> HotPotResponse.ok() }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                client.post("/p/save-me") { setBody("{}") }
                client.post("/p/skip-me") { setBody("{}") }
            }

            // assert
            val saved = storage.findRequests()
            saved.size shouldBe 1
            saved.first().path shouldBe "/p/save-me"
        }

        test("invalid auth token returns 401") {
            // arrange
            val scope =
                startScope {
                    listen(path = "/p") {
                        post(path = "/secure", auth = setOf(TokenAuthentication("secret"))) { _ ->
                            HotPotResponse.ok()
                        }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                val response =
                    client
                    .post("/p/secure") {
                        header(HttpHeaders.Authorization, "Bearer wrong")
                        setBody("{}")
                    }

                // assert
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("valid auth token returns handler response") {
            // arrange
            val scope =
                startScope {
                    listen(path = "/p") {
                        post(path = "/secure", auth = setOf(TokenAuthentication("secret"))) { _ ->
                            HotPotResponse.ok()
                        }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                val response =
                    client
                    .post("/p/secure") {
                        header(HttpHeaders.Authorization, "Bearer secret")
                        setBody("{}")
                    }

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("invalid HMAC signature returns 400") {
            // arrange
            val scope =
                startScope {
                    listen(path = "/p") {
                        post(path = "/signed", signature = setOf(HMACSignatureValidation("my-secret"))) { _ ->
                            HotPotResponse.ok()
                        }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                val response =
                    client
                    .post("/p/signed") {
                        setBody("{}")
                        header("X-Hub-Signature-256", "sha256=invalidsig")
                    }

                // assert
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("valid HMAC signature passes through to handler") {
            // arrange
            val secret = "my-secret"
            val body = """{"event":"test"}"""
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            val sig = "sha256=" + mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }

            val scope =
                startScope {
                    listen(path = "/p") {
                        post(path = "/signed", signature = setOf(HMACSignatureValidation(secret))) { _ ->
                            HotPotResponse.ok()
                        }
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                val response =
                    client
                    .post("/p/signed") {
                        setBody(body)
                        header("X-Hub-Signature-256", sig)
                    }

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET route is registered and returns 200") {
            // arrange
            val scope =
                startScope {
                    listen(path = "/p") {
                        get(path = "/health")
                    }
                }

            // act
            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                val response = client.get("/p/health")

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }
    })

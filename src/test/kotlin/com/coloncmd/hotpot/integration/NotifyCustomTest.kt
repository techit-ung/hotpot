package com.coloncmd.hotpot.integration

import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel

class NotifyCustomTest :
    FunSpec({

        test("custom notify handler can make multiple outgoing calls") {
            val calledUrls = mutableListOf<String>()

            val mockEngine =
                MockEngine { req ->
                    calledUrls += req.url.toString()
                    respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
                }

            val scope =
                StartScope(InMemoryStorage(), NotificationService(mockEngine)).apply {
                    listen(path = "/paymob") {
                        notify(path = "/complex-scenario") { req ->
                            client.post("http://service-a/cb") { setBody(req.body) }
                            client.post("http://service-b/cb") { setBody(req.body) }
                        }
                    }
                }

            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                client
                    .post("/paymob/complex-scenario") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"event":"done"}""")
                    }.status shouldBe HttpStatusCode.Accepted
            }

            calledUrls shouldBe listOf("http://service-a/cb", "http://service-b/cb")
        }

        test("custom notify handler receives the request body") {
            var receivedBody = ""

            val mockEngine =
                MockEngine { req ->
                    receivedBody = req.body.toByteArray().decodeToString()
                    respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
                }

            val scope =
                StartScope(InMemoryStorage(), NotificationService(mockEngine)).apply {
                    listen(path = "/p") {
                        notify(path = "/forward") { req ->
                            client.post("http://target/cb") { setBody(req.body) }
                        }
                    }
                }

            testApplication {
                application { HotPotServer.configureApplication(this, scope) }
                client.post("/p/forward") {
                    setBody("""{"custom":"payload"}""")
                }
            }

            receivedBody shouldBe """{"custom":"payload"}"""
        }
    })

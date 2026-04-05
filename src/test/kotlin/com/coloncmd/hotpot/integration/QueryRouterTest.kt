package com.coloncmd.hotpot.integration

import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

class QueryRouterTest :
    FunSpec({

        fun appWithRoute(storage: InMemoryStorage = InMemoryStorage()): ApplicationTestBuilder.() -> Unit =
            {
                val scope =
                    StartScope(storage).apply {
                        listen(path = "/p") {
                            post(path = "/event") { _ -> HotPotResponse.ok() }
                        }
                    }
                application { HotPotServer.configureApplication(this, scope) }
            }

        test("GET /hotpot/requests returns empty list initially") {
            // arrange
            testApplication {
                appWithRoute()()

                // act
                val body = client.get("/hotpot/requests").bodyAsText()

                // assert
                body shouldBe "[]"
            }
        }

        test("GET /hotpot/requests returns saved request after POST") {
            // arrange
            val storage = InMemoryStorage()
            testApplication {
                appWithRoute(storage)()
                client.post("/p/event") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

                // act
                val body = client.get("/hotpot/requests").bodyAsText()

                // assert
                body shouldContain "/p/event"
            }
        }

        test("GET /hotpot/requests?path= filters by path") {
            // arrange
            val storage = InMemoryStorage()
            testApplication {
                appWithRoute(storage)()
                client.post("/p/event") { setBody("{}") }

                // act
                val matching = client.get("/hotpot/requests?path=/p/event").bodyAsText()
                val nonMatching = client.get("/hotpot/requests?path=/other").bodyAsText()

                // assert
                matching shouldContain "/p/event"
                nonMatching shouldBe "[]"
            }
        }

        test("GET /hotpot/requests/{id} returns specific request") {
            // arrange
            val storage = InMemoryStorage()
            testApplication {
                appWithRoute(storage)()
                client.post("/p/event") { setBody("{}") }
                val id = storage.findRequests().first().id

                // act
                val response = client.get("/hotpot/requests/$id")

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /hotpot/requests/{id} returns 404 for unknown id") {
            // arrange
            testApplication {
                appWithRoute()()

                // act
                val response = client.get("/hotpot/requests/no-such-id")

                // assert
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("GET /hotpot/requests/{id}/response returns paired response") {
            // arrange
            val storage = InMemoryStorage()
            testApplication {
                appWithRoute(storage)()
                client.post("/p/event") { setBody("{}") }
                val id = storage.findRequests().first().id

                // act
                val response = client.get("/hotpot/requests/$id/response")

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("DELETE /hotpot/requests clears all saved data") {
            // arrange
            val storage = InMemoryStorage()
            testApplication {
                appWithRoute(storage)()
                client.post("/p/event") { setBody("{}") }

                // act
                val deleteResponse = client.delete("/hotpot/requests")
                val body = client.get("/hotpot/requests").bodyAsText()

                // assert
                deleteResponse.status shouldBe HttpStatusCode.NoContent
                body shouldBe "[]"
            }
        }
    })

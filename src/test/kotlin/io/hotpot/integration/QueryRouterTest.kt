package io.hotpot.integration

import io.hotpot.dsl.StartScope
import io.hotpot.model.HotPotResponse
import io.hotpot.server.HotPotServer
import io.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class QueryRouterTest : FunSpec({

    fun appWithRoute(storage: InMemoryStorage = InMemoryStorage()): ApplicationTestBuilder.() -> Unit = {
        val scope = StartScope(storage).apply {
            listen(path = "/p") {
                post(path = "/event") { _ -> HotPotResponse.ok() }
            }
        }
        application { HotPotServer.configureApplication(this, scope) }
    }

    test("GET /hotpot/requests returns empty list initially") {
        testApplication {
            appWithRoute()()
            client.get("/hotpot/requests").bodyAsText() shouldBe "[]"
        }
    }

    test("GET /hotpot/requests returns saved request after POST") {
        val storage = InMemoryStorage()
        testApplication {
            appWithRoute(storage)()
            client.post("/p/event") { contentType(ContentType.Application.Json); setBody("{}") }
            val body = client.get("/hotpot/requests").bodyAsText()
            body shouldContain "/p/event"
        }
    }

    test("GET /hotpot/requests?path= filters by path") {
        val storage = InMemoryStorage()
        testApplication {
            appWithRoute(storage)()
            client.post("/p/event") { setBody("{}") }
            val matching = client.get("/hotpot/requests?path=/p/event").bodyAsText()
            val nonMatching = client.get("/hotpot/requests?path=/other").bodyAsText()
            matching shouldContain "/p/event"
            nonMatching shouldBe "[]"
        }
    }

    test("GET /hotpot/requests/{id} returns specific request") {
        val storage = InMemoryStorage()
        testApplication {
            appWithRoute(storage)()
            client.post("/p/event") { setBody("{}") }
            val id = storage.findRequests().first().id
            client.get("/hotpot/requests/$id").status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /hotpot/requests/{id} returns 404 for unknown id") {
        testApplication {
            appWithRoute()()
            client.get("/hotpot/requests/no-such-id").status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /hotpot/requests/{id}/response returns paired response") {
        val storage = InMemoryStorage()
        testApplication {
            appWithRoute(storage)()
            client.post("/p/event") { setBody("{}") }
            val id = storage.findRequests().first().id
            client.get("/hotpot/requests/$id/response").status shouldBe HttpStatusCode.OK
        }
    }

    test("DELETE /hotpot/requests clears all saved data") {
        val storage = InMemoryStorage()
        testApplication {
            appWithRoute(storage)()
            client.post("/p/event") { setBody("{}") }
            client.delete("/hotpot/requests").status shouldBe HttpStatusCode.NoContent
            client.get("/hotpot/requests").bodyAsText() shouldBe "[]"
        }
    }
})

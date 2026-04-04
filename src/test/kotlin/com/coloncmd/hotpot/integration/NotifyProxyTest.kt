package com.coloncmd.hotpot.integration

import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*

class NotifyProxyTest : FunSpec({

    test("POST to notify proxy path forwards body to configured target") {
        var capturedUrl = ""
        var capturedBody = ""

        val mockEngine = MockEngine { req ->
            capturedUrl = req.url.toString()
            capturedBody = req.body.toByteArray().decodeToString()
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
        }

        val scope = StartScope(InMemoryStorage(), NotificationService(mockEngine)).apply {
            listen(path = "/paymob") {
                post(path = "/callback") { _ -> HotPotResponse.ok() }
                notify(path = "/capture-succeeded", target = "http://service/webhook")
            }
        }

        testApplication {
            application { HotPotServer.configureApplication(this, scope) }
            val response = client.post("/paymob/capture-succeeded") {
                contentType(ContentType.Application.Json)
                setBody("""{"event":"capture_succeeded"}""")
            }
            response.status shouldBe HttpStatusCode.Accepted
        }

        capturedUrl shouldBe "http://service/webhook"
        capturedBody shouldContain "capture_succeeded"
    }

    test("notify proxy forwards custom headers to target") {
        var capturedHeader = ""

        val mockEngine = MockEngine { req ->
            capturedHeader = req.headers["X-Provider"] ?: ""
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
        }

        val scope = StartScope(InMemoryStorage(), NotificationService(mockEngine)).apply {
            listen(path = "/paymob") {
                notify(
                    path = "/trigger",
                    target = "http://service/webhook",
                    headers = mapOf("X-Provider" to "paymob"),
                )
            }
        }

        testApplication {
            application { HotPotServer.configureApplication(this, scope) }
            client.post("/paymob/trigger") { setBody("{}") }
        }

        capturedHeader shouldBe "paymob"
    }
})

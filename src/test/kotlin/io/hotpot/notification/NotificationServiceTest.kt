package io.hotpot.notification

import io.hotpot.model.WebhookRequest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*

class NotificationServiceTest : FunSpec({

    fun request(body: String = """{"event":"test"}""") = WebhookRequest(
        id = "req-1",
        path = "/paymob/callback",
        method = "POST",
        headers = emptyMap(),
        body = body,
        receivedAt = System.currentTimeMillis(),
    )

    test("Proxy form forwards incoming body to target URL") {
        var capturedUrl = ""
        var capturedBody = ""

        val engine = MockEngine { req ->
            capturedUrl = req.url.toString()
            capturedBody = req.body.toByteArray().decodeToString()
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
        }

        val service = NotificationService(engine)
        val definition = NotifyDefinition.Proxy(
            path = "/trigger",
            target = "http://service/webhooks",
        )

        service.dispatch(definition, request("""{"event":"capture_succeeded"}"""))

        capturedUrl shouldBe "http://service/webhooks"
        capturedBody shouldContain "capture_succeeded"
    }

    test("Proxy form forwards custom headers to target") {
        var capturedHeader = ""

        val engine = MockEngine { req ->
            capturedHeader = req.headers["X-Provider"] ?: ""
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
        }

        val service = NotificationService(engine)
        val definition = NotifyDefinition.Proxy(
            path = "/trigger",
            target = "http://service/webhooks",
            headers = mapOf("X-Provider" to "paymob"),
        )

        service.dispatch(definition, request())
        capturedHeader shouldBe "paymob"
    }

    test("Custom form invokes handler with NotifyContext and request") {
        var handlerCalled = false
        var receivedPath = ""

        val engine = MockEngine { respond(content = ByteReadChannel(""), status = HttpStatusCode.OK) }
        val service = NotificationService(engine)

        val definition = NotifyDefinition.Custom(path = "/complex") { req ->
            handlerCalled = true
            receivedPath = req.path
        }

        service.dispatch(definition, request())

        handlerCalled shouldBe true
        receivedPath shouldBe "/paymob/callback"
    }

    test("Custom form can make multiple outgoing calls") {
        val calledUrls = mutableListOf<String>()

        val engine = MockEngine { req ->
            calledUrls += req.url.toString()
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK)
        }

        val service = NotificationService(engine)

        val definition = NotifyDefinition.Custom(path = "/multi") { req ->
            client.post("http://service-a/cb") { }
            client.post("http://service-b/cb") { }
        }

        service.dispatch(definition, request())

        calledUrls shouldBe listOf("http://service-a/cb", "http://service-b/cb")
    }
})

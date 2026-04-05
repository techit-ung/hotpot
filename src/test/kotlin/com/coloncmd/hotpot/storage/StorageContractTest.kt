package com.coloncmd.hotpot.storage

import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.model.WebhookResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Clock

abstract class StorageContractTest : FunSpec() {
    abstract fun storage(): Storage

    private fun request(
        path: String = "/test",
        method: String = "POST",
    ) = WebhookRequest(
        id = "",
        path = path,
        method = method,
        headers = emptyMap(),
        body = """{"hello":"world"}""",
        receivedAt = Clock.System.now(),
    )

    init {
        test("saveRequest returns a non-blank ID") {
            val id = storage().saveRequest(request())
            id.isNotBlank() shouldBe true
        }

        test("findRequest returns saved request by ID") {
            val storage = storage()
            val id = storage.saveRequest(request(path = "/find-me"))
            val found = storage.findRequest(id)
            found.shouldNotBeNull()
            found.path shouldBe "/find-me"
            found.id shouldBe id
        }

        test("findRequest returns null for unknown ID") {
            storage().findRequest("no-such-id").shouldBeNull()
        }

        test("findRequests returns all saved requests") {
            val storage = storage()
            storage.saveRequest(request(path = "/a"))
            storage.saveRequest(request(path = "/b"))
            storage.findRequests() shouldHaveSize 2
        }

        test("findRequests filters by path") {
            val storage = storage()
            storage.saveRequest(request(path = "/alpha"))
            storage.saveRequest(request(path = "/beta"))
            val results = storage.findRequests(path = "/alpha")
            results shouldHaveSize 1
            results.first().path shouldBe "/alpha"
        }

        test("findRequests filters by method") {
            val storage = storage()
            storage.saveRequest(request(method = "POST"))
            storage.saveRequest(request(method = "GET"))
            val results = storage.findRequests(method = "GET")
            results shouldHaveSize 1
            results.first().method shouldBe "GET"
        }

        test("findRequests respects limit") {
            val storage = storage()
            repeat(5) { storage.saveRequest(request()) }
            storage.findRequests(limit = 3) shouldHaveSize 3
        }

        test("saveResponse links to request; findResponseFor retrieves it") {
            val storage = storage()
            val requestId = storage.saveRequest(request())
            val response = WebhookResponse(requestId = requestId, status = 200, body = "{}", sentAt = Clock.System.now())
            storage.saveResponse(requestId, response)
            val found = storage.findResponseFor(requestId)
            found.shouldNotBeNull()
            found.status shouldBe 200
            found.requestId shouldBe requestId
        }

        test("findResponseFor returns null when no response saved") {
            val storage = storage()
            val id = storage.saveRequest(request())
            storage.findResponseFor(id).shouldBeNull()
        }

        test("clear removes all requests and responses") {
            val storage = storage()
            val id = storage.saveRequest(request())
            storage.saveResponse(id, WebhookResponse(id, 200, "{}", Clock.System.now()))
            storage.clear()
            storage.findRequests() shouldHaveSize 0
            storage.findResponseFor(id).shouldBeNull()
        }
    }
}

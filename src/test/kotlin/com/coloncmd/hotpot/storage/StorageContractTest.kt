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
            // arrange
            val storage = storage()

            // act
            val id = storage.saveRequest(request())

            // assert
            id.isNotBlank() shouldBe true
        }

        test("findRequest returns saved request by ID") {
            // arrange
            val storage = storage()
            val id = storage.saveRequest(request(path = "/find-me"))

            // act
            val found = storage.findRequest(id)

            // assert
            found.shouldNotBeNull()
            found.path shouldBe "/find-me"
            found.id shouldBe id
        }

        test("findRequest returns null for unknown ID") {
            // arrange
            val storage = storage()

            // act
            val found = storage.findRequest("no-such-id")

            // assert
            found.shouldBeNull()
        }

        test("findRequests returns all saved requests") {
            // arrange
            val storage = storage()
            storage.saveRequest(request(path = "/a"))
            storage.saveRequest(request(path = "/b"))

            // act
            val results = storage.findRequests()

            // assert
            results shouldHaveSize 2
        }

        test("findRequests filters by path") {
            // arrange
            val storage = storage()
            storage.saveRequest(request(path = "/alpha"))
            storage.saveRequest(request(path = "/beta"))

            // act
            val results = storage.findRequests(path = "/alpha")

            // assert
            results shouldHaveSize 1
            results.first().path shouldBe "/alpha"
        }

        test("findRequests filters by method") {
            // arrange
            val storage = storage()
            storage.saveRequest(request(method = "POST"))
            storage.saveRequest(request(method = "GET"))

            // act
            val results = storage.findRequests(method = "GET")

            // assert
            results shouldHaveSize 1
            results.first().method shouldBe "GET"
        }

        test("findRequests respects limit") {
            // arrange
            val storage = storage()
            repeat(5) { storage.saveRequest(request()) }

            // act
            val results = storage.findRequests(limit = 3)

            // assert
            results shouldHaveSize 3
        }

        test("saveResponse links to request; findResponseFor retrieves it") {
            // arrange
            val storage = storage()
            val requestId = storage.saveRequest(request())
            val response = WebhookResponse(requestId = requestId, status = 200, body = "{}", sentAt = Clock.System.now())

            // act
            storage.saveResponse(requestId, response)
            val found = storage.findResponseFor(requestId)

            // assert
            found.shouldNotBeNull()
            found.status shouldBe 200
            found.requestId shouldBe requestId
        }

        test("findResponseFor returns null when no response saved") {
            // arrange
            val storage = storage()
            val id = storage.saveRequest(request())

            // act
            val found = storage.findResponseFor(id)

            // assert
            found.shouldBeNull()
        }

        test("clear removes all requests and responses") {
            // arrange
            val storage = storage()
            val id = storage.saveRequest(request())
            storage.saveResponse(id, WebhookResponse(id, 200, "{}", Clock.System.now()))

            // act
            storage.clear()

            // assert
            storage.findRequests() shouldHaveSize 0
            storage.findResponseFor(id).shouldBeNull()
        }
    }
}

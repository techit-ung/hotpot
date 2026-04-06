package com.coloncmd.hotpot.standalone.template

import com.coloncmd.hotpot.model.WebhookRequest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

class TemplateEngineTest :
    FunSpec({
        fun request(
            id: String = "req-123",
            path: String = "/payments",
            method: String = "POST",
            headers: Map<String, List<String>> = emptyMap(),
            body: String = "{}",
        ) = WebhookRequest(id, path, method, headers, body, Instant.fromEpochSeconds(0))

        test("renders request.id") {
            // arrange
            val template = """{"id": "{{request.id}}"}"""
            val req = request(id = "abc-456")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"id": "abc-456"}"""
        }

        test("renders request.path") {
            // arrange
            val template = """{"path": "{{request.path}}"}"""
            val req = request(path = "/orders")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"path": "/orders"}"""
        }

        test("renders request.method") {
            // arrange
            val template = """{"method": "{{request.method}}"}"""
            val req = request(method = "GET")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"method": "GET"}"""
        }

        test("renders request.body raw") {
            // arrange
            val template = """{"payload": {{request.body}}}"""
            val req = request(body = """{"amount": 99}""")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"payload": {"amount": 99}}"""
        }

        test("renders request.body.field from JSON body") {
            // arrange
            val template = """{"amount": "{{request.body.amount}}"}"""
            val req = request(body = """{"amount": "42.00", "currency": "USD"}""")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"amount": "42.00"}"""
        }

        test("renders empty string when request.body.field is absent") {
            // arrange
            val template = """{"missing": "{{request.body.nope}}"}"""
            val req = request(body = """{"amount": "42.00"}""")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"missing": ""}"""
        }

        test("renders request.headers.Name") {
            // arrange
            val template = """{"source": "{{request.headers.X-Source}}"}"""
            val req = request(headers = mapOf("X-Source" to listOf("hotpot")))

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"source": "hotpot"}"""
        }

        test("renders empty string when header is absent") {
            // arrange
            val template = """{"h": "{{request.headers.X-Missing}}"}"""
            val req = request(headers = emptyMap())

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"h": ""}"""
        }

        test("substitutes multiple variables in one template") {
            // arrange
            val template = """{"id": "{{request.id}}", "path": "{{request.path}}"}"""
            val req = request(id = "x1", path = "/foo")

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"id": "x1", "path": "/foo"}"""
        }

        test("renders empty string for unknown variable path") {
            // arrange
            val template = """{"x": "{{request.unknown}}"}"""
            val req = request()

            // act
            val result = TemplateEngine.render(template, req)

            // assert
            result shouldBe """{"x": ""}"""
        }
    })

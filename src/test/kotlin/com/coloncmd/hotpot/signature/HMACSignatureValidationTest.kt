package com.coloncmd.hotpot.signature

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.utils.io.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HMACSignatureValidationTest : FunSpec({

    val secret = "test-secret"
    val strategy = HMACSignatureValidation(secret = secret)

    fun computeHmac(body: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body).joinToString("") { "%02x".format(it) }
    }

    fun ApplicationTestBuilder.withSignatureRoute() = routing {
        post("/test") {
            val raw = call.receiveChannel().toByteArray()
            when (strategy.validate(call, raw)) {
                SignatureResult.Valid -> call.respond(HttpStatusCode.OK)
                is SignatureResult.Invalid -> call.respond(HttpStatusCode.BadRequest)
            }
        }
    }

    test("valid HMAC signature passes") {
        val body = """{"event":"test"}"""
        testApplication {
            withSignatureRoute()
            client.post("/test") {
                setBody(body)
                header("X-Hub-Signature-256", computeHmac(body.toByteArray()))
            }.status shouldBe HttpStatusCode.OK
        }
    }

    test("tampered body is rejected") {
        val body = """{"event":"test"}"""
        testApplication {
            withSignatureRoute()
            client.post("/test") {
                setBody("""{"event":"tampered"}""")
                header("X-Hub-Signature-256", computeHmac(body.toByteArray()))
            }.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("wrong secret is rejected") {
        val body = """{"event":"test"}"""
        testApplication {
            withSignatureRoute()
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec("wrong-secret".toByteArray(), "HmacSHA256"))
            val wrongSig = "sha256=" + mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
            client.post("/test") {
                setBody(body)
                header("X-Hub-Signature-256", wrongSig)
            }.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("missing signature header is rejected") {
        testApplication {
            withSignatureRoute()
            client.post("/test") {
                setBody("""{"event":"test"}""")
            }.status shouldBe HttpStatusCode.BadRequest
        }
    }
})

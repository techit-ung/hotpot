package io.hotpot.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*

class TokenAuthenticationTest : FunSpec({

    fun ApplicationTestBuilder.withAuthRoute(token: String) = routing {
        get("/test") {
            when (TokenAuthentication(token).validate(call)) {
                AuthResult.Success -> call.respond(HttpStatusCode.OK)
                is AuthResult.Failure -> call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

    test("valid Bearer token passes") {
        testApplication {
            withAuthRoute("secret")
            client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer secret")
            }.status shouldBe HttpStatusCode.OK
        }
    }

    test("wrong token is rejected") {
        testApplication {
            withAuthRoute("secret")
            client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer wrong")
            }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("missing Authorization header is rejected") {
        testApplication {
            withAuthRoute("secret")
            client.get("/test").status shouldBe HttpStatusCode.Unauthorized
        }
    }
})

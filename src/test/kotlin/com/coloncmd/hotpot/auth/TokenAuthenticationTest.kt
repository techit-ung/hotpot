package com.coloncmd.hotpot.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

class TokenAuthenticationTest :
    FunSpec({

        fun ApplicationTestBuilder.withAuthRoute(token: String) =
            routing {
                get("/test") {
                    when (TokenAuthentication(token).validate(call)) {
                        AuthResult.Success -> call.respond(HttpStatusCode.OK)
                        is AuthResult.Failure -> call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }

        test("valid Bearer token passes") {
            // arrange
            testApplication {
                withAuthRoute("secret")

                // act
                val response =
                    client
                    .get("/test") {
                        header(HttpHeaders.Authorization, "Bearer secret")
                    }

                // assert
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("wrong token is rejected") {
            // arrange
            testApplication {
                withAuthRoute("secret")

                // act
                val response =
                    client
                    .get("/test") {
                        header(HttpHeaders.Authorization, "Bearer wrong")
                    }

                // assert
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("missing Authorization header is rejected") {
            // arrange
            testApplication {
                withAuthRoute("secret")

                // act
                val response = client.get("/test")

                // assert
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    })

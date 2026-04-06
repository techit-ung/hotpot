package com.coloncmd.hotpot.standalone.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConfigLoaderTest :
    FunSpec({
        test("parses minimal YAML with defaults") {
            // arrange
            val yaml = "port: 9090"

            // act
            val config = ConfigLoader.load(yaml)

            // assert
            config.port shouldBe 9090
            config.groups shouldBe emptyList()
        }

        test("parses default port when omitted") {
            // arrange
            val yaml = "groups: []"

            // act
            val config = ConfigLoader.load(yaml)

            // assert
            config.port shouldBe 8080
        }

        test("parses full config with route, auth, signature, notify") {
            // arrange
            val yaml =
                """
                port: 8080
                groups:
                  - path: /payments
                    save: true
                    routes:
                      - method: POST
                        path: ""
                        auth:
                          - type: token
                            token: "secret"
                        signature:
                          - type: hmac
                            secret: "hmac-secret" # pragma: allowlist secret
                            headerName: "X-Sig"
                            algorithm: "HmacSHA256"
                            prefix: "sha256="
                        response:
                          status: 202
                          body: '{"id": "{{request.id}}"}'
                    notify:
                      - path: ""
                        target: "http://localhost:9090/audit"
                        headers:
                          X-Source: hotpot
                """.trimIndent()

            // act
            val config = ConfigLoader.load(yaml)

            // assert
            config.port shouldBe 8080
            val group = config.groups.single()
            group.path shouldBe "/payments"
            group.save shouldBe true

            val route = group.routes.single()
            route.method shouldBe "POST"
            route.auth.single() shouldBe AuthConfig.Token("secret")
            route.signature.single() shouldBe
                SignatureConfig.Hmac(
                    secret = "hmac-secret", // pragma: allowlist secret
                    headerName = "X-Sig",
                    algorithm = "HmacSHA256",
                    prefix = "sha256=",
                )
            route.response.status shouldBe 202

            val notify = group.notify.single()
            notify.path shouldBe ""
            notify.target shouldBe "http://localhost:9090/audit"
            notify.headers shouldBe mapOf("X-Source" to "hotpot")
        }

        test("substitutes env vars before parsing") {
            // arrange
            val yaml =
                """
                groups:
                  - path: /x
                    routes:
                      - method: POST
                        auth:
                          - type: token
                            token: "${'$'}{MY_TOKEN}"
                """.trimIndent()
            val envLookup: (String) -> String? = { name -> if (name == "MY_TOKEN") "injected-token" else null }

            // act
            val config = ConfigLoader.load(yaml, envLookup)

            // assert
            val token =
                config.groups
                    .single()
                    .routes
                    .single()
                    .auth
                    .single() as AuthConfig.Token
            token.token shouldBe "injected-token"
        }

        test("throws for missing required env var") {
            // arrange
            val yaml = "port: \${MISSING_VAR}"
            val envLookup: (String) -> String? = { null }

            // act / assert
            shouldThrow<IllegalStateException> {
                ConfigLoader.load(yaml, envLookup)
            }
        }
    })

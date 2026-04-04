package com.example

import com.coloncmd.hotpot.auth.TokenAuthentication
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.signature.HMACSignatureValidation
import com.coloncmd.hotpot.start
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Example: mock payment webhook server using hotpot.
 *
 * Endpoints:
 *   POST /payments       — accepts payment events (requires Bearer token + HMAC signature)
 *   POST /payments/refunds — accepts refund events (token auth only)
 *   GET  /payments       — query stored payment requests
 *
 * The server also forwards every received payment event to a local audit service.
 */
fun main() {
    val apiToken = "secret-api-token"
    val webhookSecret = "my-hmac-secret"

    start(port = 8080) {
        listen(path = "/payments") {
            post(
                path = "",
                auth = setOf(TokenAuthentication(apiToken)),
                signature = setOf(HMACSignatureValidation(secret = webhookSecret)),
            ) { request ->
                println("Payment received: ${request.body}")
                HotPotResponse.accepted(
                    buildJsonObject {
                        put("message", "Payment event received")
                        put("id", request.id)
                    }
                )
            }

            post(
                path = "/refunds",
                auth = setOf(TokenAuthentication(apiToken)),
            ) { request ->
                println("Refund received: ${request.body}")
                HotPotResponse.ok(
                    buildJsonObject {
                        put("message", "Refund event received")
                        put("id", request.id)
                    }
                )
            }

            // Forward every payment event to a local audit service
            notify(
                path = "",
                target = "http://localhost:9090/audit/payments",
                headers = mapOf("X-Source" to "hotpot-example"),
            )
        }
    }
}

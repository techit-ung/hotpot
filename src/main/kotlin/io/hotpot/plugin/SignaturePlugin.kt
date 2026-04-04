package io.hotpot.plugin

import io.hotpot.signature.SignatureResult
import io.hotpot.signature.SignatureStrategy
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/** Validates all [strategies] against the call and [rawBody]. Returns true if all pass; responds with 400 and returns false on failure. */
suspend fun ApplicationCall.validateSignature(strategies: Set<SignatureStrategy>, rawBody: ByteArray): Boolean {
    for (strategy in strategies) {
        when (val result = strategy.validate(this, rawBody)) {
            is SignatureResult.Invalid -> {
                respond(HttpStatusCode.BadRequest, result.reason)
                return false
            }
            SignatureResult.Valid -> Unit
        }
    }
    return true
}

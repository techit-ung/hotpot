package com.coloncmd.hotpot.plugin

import com.coloncmd.hotpot.signature.SignatureResult
import com.coloncmd.hotpot.signature.SignatureStrategy
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/** Validates all [strategies] against the call and [rawBody]. Returns true if all pass; responds with 400 and returns false on failure. */
suspend fun ApplicationCall.validateSignature(
    strategies: Set<SignatureStrategy>,
    rawBody: ByteArray,
): Boolean {
    for (strategy in strategies) {
        when (val result = strategy.validate(this, rawBody)) {
            is SignatureResult.Invalid -> {
                respond(HttpStatusCode.BadRequest, result.reason)
                return false
            }

            SignatureResult.Valid -> {
                Unit
            }
        }
    }
    return true
}

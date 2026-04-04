package com.coloncmd.hotpot.signature

import io.ktor.server.application.*

sealed interface SignatureResult {
    data object Valid : SignatureResult
    data class Invalid(val reason: String) : SignatureResult
}

interface SignatureStrategy {
    suspend fun validate(call: ApplicationCall, rawBody: ByteArray): SignatureResult
}

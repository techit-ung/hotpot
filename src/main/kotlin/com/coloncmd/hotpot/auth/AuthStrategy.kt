package com.coloncmd.hotpot.auth

import io.ktor.server.application.*

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val reason: String) : AuthResult
}

interface AuthStrategy {
    suspend fun validate(call: ApplicationCall): AuthResult
}

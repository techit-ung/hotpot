package com.coloncmd.hotpot.auth

import io.ktor.http.*
import io.ktor.server.application.*

data class TokenAuthentication(val token: String) : AuthStrategy {
    override suspend fun validate(call: ApplicationCall): AuthResult {
        val header = call.request.headers[HttpHeaders.Authorization]
        return if (header == "Bearer $token") AuthResult.Success
        else AuthResult.Failure("Invalid or missing Bearer token")
    }
}

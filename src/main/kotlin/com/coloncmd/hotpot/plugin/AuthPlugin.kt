package com.coloncmd.hotpot.plugin

import com.coloncmd.hotpot.auth.AuthResult
import com.coloncmd.hotpot.auth.AuthStrategy
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/** Validates all [strategies] against the call. Returns true if all pass; responds with 401 and returns false on first failure. */
suspend fun ApplicationCall.validateAuth(strategies: Set<AuthStrategy>): Boolean {
    for (strategy in strategies) {
        when (val result = strategy.validate(this)) {
            is AuthResult.Failure -> {
                respond(HttpStatusCode.Unauthorized, result.reason)
                return false
            }

            AuthResult.Success -> {
                Unit
            }
        }
    }
    return true
}

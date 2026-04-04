package io.hotpot.dsl

import io.hotpot.storage.Storage
import io.ktor.server.application.*

class HandlerContext(
    val call: ApplicationCall,
    val storage: Storage,
)

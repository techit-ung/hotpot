package com.coloncmd.hotpot.dsl

import com.coloncmd.hotpot.storage.Storage
import io.ktor.server.application.*

class HandlerContext(
    val call: ApplicationCall,
    val storage: Storage,
)

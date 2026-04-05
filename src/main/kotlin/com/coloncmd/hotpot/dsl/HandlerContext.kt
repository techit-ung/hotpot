package com.coloncmd.hotpot.dsl

import com.coloncmd.hotpot.storage.Storage
import io.ktor.server.application.ApplicationCall

class HandlerContext(
    val call: ApplicationCall,
    val storage: Storage,
)

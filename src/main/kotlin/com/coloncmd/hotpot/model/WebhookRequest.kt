package com.coloncmd.hotpot.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class WebhookRequest(
    val id: String,
    val path: String,
    val method: String,
    val headers: Map<String, List<String>>,
    val body: String,
    val receivedAt: Instant,
)

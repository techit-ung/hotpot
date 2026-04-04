package com.coloncmd.hotpot.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class WebhookRequest(
    val id: String,
    val path: String,
    val method: String,
    val headers: Map<String, List<String>>,
    val body: String,
    val receivedAt: Instant,
)

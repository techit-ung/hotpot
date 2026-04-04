package com.coloncmd.hotpot.model

import kotlinx.serialization.Serializable

@Serializable
data class WebhookRequest(
    val id: String,
    val path: String,
    val method: String,
    val headers: Map<String, List<String>>,
    val body: String,
    val receivedAt: Long, // epoch millis
)

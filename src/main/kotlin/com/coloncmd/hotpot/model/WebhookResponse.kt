package com.coloncmd.hotpot.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class WebhookResponse(
    val requestId: String,
    val status: Int,
    val body: String,
    val sentAt: Instant,
)

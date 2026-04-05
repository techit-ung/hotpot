package com.coloncmd.hotpot.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class WebhookResponse(
    val requestId: String,
    val status: Int,
    val body: String,
    val sentAt: Instant,
)

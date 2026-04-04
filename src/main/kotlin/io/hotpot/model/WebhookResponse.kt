package io.hotpot.model

import kotlinx.serialization.Serializable

@Serializable
data class WebhookResponse(
    val requestId: String,
    val status: Int,
    val body: String,
    val sentAt: Long, // epoch millis
)

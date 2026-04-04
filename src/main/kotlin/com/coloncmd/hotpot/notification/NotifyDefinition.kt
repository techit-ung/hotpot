package com.coloncmd.hotpot.notification

import com.coloncmd.hotpot.model.WebhookRequest

sealed class NotifyDefinition {
    abstract val path: String

    data class Proxy(
        override val path: String,
        val target: String,
        val headers: Map<String, String> = emptyMap(),
    ) : NotifyDefinition()

    data class Custom(
        override val path: String,
        val handler: suspend NotifyContext.(WebhookRequest) -> Unit,
    ) : NotifyDefinition()
}

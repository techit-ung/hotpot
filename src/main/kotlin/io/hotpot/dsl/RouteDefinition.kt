package io.hotpot.dsl

import io.hotpot.auth.AuthStrategy
import io.hotpot.model.HotPotResponse
import io.hotpot.model.WebhookRequest
import io.hotpot.signature.SignatureStrategy

sealed class RouteDefinition {
    abstract val path: String
    abstract val auth: Set<AuthStrategy>
    abstract val signature: Set<SignatureStrategy>
    abstract val saveRequestResponse: Boolean?  // null = inherit from RouteGroup
    abstract val handler: suspend HandlerContext.(WebhookRequest) -> HotPotResponse

    data class Post(
        override val path: String,
        override val auth: Set<AuthStrategy> = emptySet(),
        override val signature: Set<SignatureStrategy> = emptySet(),
        override val saveRequestResponse: Boolean? = null,
        override val handler: suspend HandlerContext.(WebhookRequest) -> HotPotResponse,
    ) : RouteDefinition()

    data class Get(
        override val path: String,
        override val auth: Set<AuthStrategy> = emptySet(),
        override val signature: Set<SignatureStrategy> = emptySet(),
        override val saveRequestResponse: Boolean? = null,
        override val handler: suspend HandlerContext.(WebhookRequest) -> HotPotResponse,
    ) : RouteDefinition()
}

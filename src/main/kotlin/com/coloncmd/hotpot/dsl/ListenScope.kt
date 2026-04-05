package com.coloncmd.hotpot.dsl

import com.coloncmd.hotpot.auth.AuthStrategy
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.notification.NotifyContext
import com.coloncmd.hotpot.notification.NotifyDefinition
import com.coloncmd.hotpot.signature.SignatureStrategy

class ListenScope internal constructor(
    internal val basePath: String,
    internal val saveRequestResponse: Boolean,
) {
    internal val routes = mutableListOf<RouteDefinition>()
    internal val notifyRoutes = mutableListOf<NotifyDefinition>()

    fun post(
        path: String,
        auth: Set<AuthStrategy> = emptySet(),
        signature: Set<SignatureStrategy> = emptySet(),
        saveRequestResponse: Boolean? = null,
        handler: suspend HandlerContext.(WebhookRequest) -> HotPotResponse,
    ) {
        routes +=
            RouteDefinition.Post(
                path = path,
                auth = auth,
                signature = signature,
                saveRequestResponse = saveRequestResponse,
                handler = handler,
            )
    }

    fun get(
        path: String,
        auth: Set<AuthStrategy> = emptySet(),
        signature: Set<SignatureStrategy> = emptySet(),
        saveRequestResponse: Boolean? = null,
        handler: suspend HandlerContext.(WebhookRequest) -> HotPotResponse = { HotPotResponse.ok() },
    ) {
        routes +=
            RouteDefinition.Get(
                path = path,
                auth = auth,
                signature = signature,
                saveRequestResponse = saveRequestResponse,
                handler = handler,
            )
    }

    fun notify(
        path: String,
        target: String,
        headers: Map<String, String> = emptyMap(),
    ) {
        notifyRoutes += NotifyDefinition.Proxy(path = path, target = target, headers = headers)
    }

    fun notify(
        path: String,
        handler: suspend NotifyContext.(WebhookRequest) -> Unit,
    ) {
        notifyRoutes += NotifyDefinition.Custom(path = path, handler = handler)
    }
}

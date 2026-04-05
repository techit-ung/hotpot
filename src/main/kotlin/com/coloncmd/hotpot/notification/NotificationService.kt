package com.coloncmd.hotpot.notification

import com.coloncmd.hotpot.model.WebhookRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class NotificationService(
    engine: HttpClientEngine,
) {
    val client = HttpClient(engine)

    companion object {
        fun create(): NotificationService = NotificationService(CIO.create())
    }

    suspend fun dispatch(
        definition: NotifyDefinition,
        request: WebhookRequest,
    ) {
        when (definition) {
            is NotifyDefinition.Proxy -> proxy(definition, request)
            is NotifyDefinition.Custom -> definition.handler.invoke(NotifyContext(client), request)
        }
    }

    private suspend fun proxy(
        definition: NotifyDefinition.Proxy,
        request: WebhookRequest,
    ) {
        client.post(definition.target) {
            contentType(ContentType.Application.Json)
            definition.headers.forEach { (k, v) -> header(k, v) }
            setBody(request.body)
        }
    }
}

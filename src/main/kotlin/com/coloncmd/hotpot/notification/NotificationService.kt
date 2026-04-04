package com.coloncmd.hotpot.notification

import com.coloncmd.hotpot.model.WebhookRequest
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*

class NotificationService(engine: HttpClientEngine) {
    val client = HttpClient(engine)

    companion object {
        fun create(): NotificationService = NotificationService(CIO.create())
    }

    suspend fun dispatch(definition: NotifyDefinition, request: WebhookRequest) {
        when (definition) {
            is NotifyDefinition.Proxy -> proxy(definition, request)
            is NotifyDefinition.Custom -> definition.handler.invoke(NotifyContext(client), request)
        }
    }

    private suspend fun proxy(definition: NotifyDefinition.Proxy, request: WebhookRequest) {
        client.post(definition.target) {
            contentType(ContentType.Application.Json)
            definition.headers.forEach { (k, v) -> header(k, v) }
            setBody(request.body)
        }
    }
}

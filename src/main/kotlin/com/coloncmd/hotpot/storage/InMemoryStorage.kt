package com.coloncmd.hotpot.storage

import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.model.WebhookResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryStorage : Storage {
    private val requests = ConcurrentHashMap<String, WebhookRequest>()
    private val responses = ConcurrentHashMap<String, WebhookResponse>()

    override suspend fun saveRequest(request: WebhookRequest): String {
        val id = UUID.randomUUID().toString()
        requests[id] = request.copy(id = id)
        return id
    }

    override suspend fun saveResponse(requestId: String, response: WebhookResponse) {
        responses[requestId] = response.copy(requestId = requestId)
    }

    override suspend fun findRequests(path: String?, method: String?, limit: Int): List<WebhookRequest> =
        requests.values
            .filter { path == null || it.path == path }
            .filter { method == null || it.method == method }
            .sortedByDescending { it.receivedAt }
            .take(limit)

    override suspend fun findRequest(id: String): WebhookRequest? = requests[id]

    override suspend fun findResponseFor(requestId: String): WebhookResponse? = responses[requestId]

    override suspend fun clear() {
        requests.clear()
        responses.clear()
    }
}

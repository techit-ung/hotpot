package com.coloncmd.hotpot.storage

import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.model.WebhookResponse

interface Storage {
    suspend fun saveRequest(request: WebhookRequest): String
    suspend fun saveResponse(requestId: String, response: WebhookResponse)
    suspend fun findRequests(path: String? = null, method: String? = null, limit: Int = 50): List<WebhookRequest>
    suspend fun findRequest(id: String): WebhookRequest?
    suspend fun findResponseFor(requestId: String): WebhookResponse?
    suspend fun clear()
}

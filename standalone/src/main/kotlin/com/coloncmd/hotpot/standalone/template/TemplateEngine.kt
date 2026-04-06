package com.coloncmd.hotpot.standalone.template

import com.coloncmd.hotpot.model.WebhookRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object TemplateEngine {
    private val TOKEN = Regex("""\{\{([^}]+)\}\}""")

    fun render(
        template: String,
        request: WebhookRequest,
    ): String =
        TOKEN.replace(template) { match ->
            val parts = match.groupValues[1].trim().split(".")
            resolve(parts, request)
        }

    private fun resolve(
        parts: List<String>,
        request: WebhookRequest,
    ): String {
        if (parts.firstOrNull() != "request") return ""
        return when (parts.getOrNull(1)) {
            "id" -> request.id
            "path" -> request.path
            "method" -> request.method
            "body" ->
                if (parts.size == 2) {
                    request.body
                } else {
                    jsonField(request.body, parts[2])
                }
            "headers" -> request.headers[parts.getOrNull(2)]?.firstOrNull() ?: ""
            else -> ""
        }
    }

    private fun jsonField(
        body: String,
        field: String,
    ): String =
        runCatching {
            (Json.parseToJsonElement(body) as? JsonObject)
                ?.get(field)
                ?.jsonPrimitive
                ?.content
        }.getOrNull() ?: ""
}

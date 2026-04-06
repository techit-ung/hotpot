package com.coloncmd.hotpot.standalone.config

import com.coloncmd.hotpot.auth.TokenAuthentication
import com.coloncmd.hotpot.buildScope
import com.coloncmd.hotpot.dsl.HandlerContext
import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.signature.HMACSignatureValidation
import com.coloncmd.hotpot.standalone.template.TemplateEngine
import com.coloncmd.hotpot.storage.Storage
import kotlinx.serialization.json.Json

object ConfigBridge {
    fun toStartScope(
        config: HotPotConfig,
        storage: Storage,
        notificationService: NotificationService = NotificationService.create(),
    ): StartScope =
        buildScope(storage, notificationService) {
            for (group in config.groups) {
                listen(path = group.path, saveRequestResponse = group.save) {
                    for (route in group.routes) {
                        val auth = route.auth.map { it.toStrategy() }.toSet()
                        val sig = route.signature.map { it.toStrategy() }.toSet()
                        val handler: suspend HandlerContext.(WebhookRequest) -> HotPotResponse = { req ->
                            val rendered = TemplateEngine.render(route.response.body, req)
                            HotPotResponse(route.response.status, Json.parseToJsonElement(rendered))
                        }
                        when (route.method.uppercase()) {
                            "POST" -> post(route.path, auth, sig, route.save, handler)
                            "GET" -> get(route.path, auth, sig, route.save, handler)
                            else -> error("Unsupported HTTP method: ${route.method}")
                        }
                    }
                    for (n in group.notify) notify(n.path, n.target, n.headers)
                }
            }
        }

    private fun AuthConfig.toStrategy() =
        when (this) {
            is AuthConfig.Token -> TokenAuthentication(token)
        }

    private fun SignatureConfig.toStrategy() =
        when (this) {
            is SignatureConfig.Hmac -> HMACSignatureValidation(secret, headerName, algorithm, prefix)
        }
}

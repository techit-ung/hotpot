package com.coloncmd.hotpot.standalone.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HotPotConfig(
    val port: Int = 8080,
    val groups: List<RouteGroupConfig> = emptyList(),
)

@Serializable
data class RouteGroupConfig(
    val path: String = "",
    val save: Boolean = true,
    val routes: List<RouteConfig> = emptyList(),
    val notify: List<NotifyConfig> = emptyList(),
)

@Serializable
data class RouteConfig(
    val method: String,
    val path: String = "",
    val save: Boolean? = null,
    val auth: List<AuthConfig> = emptyList(),
    val signature: List<SignatureConfig> = emptyList(),
    val response: ResponseConfig = ResponseConfig(),
)

@Serializable
data class ResponseConfig(
    val status: Int = 200,
    val body: String = "{}",
)

@Serializable
sealed class AuthConfig {
    @Serializable
    @SerialName("token")
    data class Token(
        val token: String,
    ) : AuthConfig()
}

@Serializable
sealed class SignatureConfig {
    @Serializable
    @SerialName("hmac")
    data class Hmac(
        val secret: String,
        val headerName: String = "X-Hub-Signature-256",
        val algorithm: String = "HmacSHA256",
        val prefix: String = "sha256=",
    ) : SignatureConfig()
}

@Serializable
data class NotifyConfig(
    val path: String = "",
    val target: String,
    val headers: Map<String, String> = emptyMap(),
)

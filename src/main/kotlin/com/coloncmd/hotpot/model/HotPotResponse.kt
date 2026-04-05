package com.coloncmd.hotpot.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
data class HotPotResponse(
    val status: Int,
    val body: JsonElement = JsonNull,
) {
    companion object {
        fun ok(body: JsonElement = JsonNull) = HotPotResponse(200, body)

        fun accepted(body: JsonElement = JsonNull) = HotPotResponse(202, body)

        fun badRequest(body: JsonElement = JsonNull) = HotPotResponse(400, body)

        fun unauthorized() = HotPotResponse(401)

        fun notFound() = HotPotResponse(404)
    }
}

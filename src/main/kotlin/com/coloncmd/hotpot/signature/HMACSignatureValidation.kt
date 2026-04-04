package com.coloncmd.hotpot.signature

import io.ktor.server.application.*
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class HMACSignatureValidation(
    val secret: String,
    val headerName: String = "X-Hub-Signature-256",
    val algorithm: String = "HmacSHA256",
    val prefix: String = "sha256=",
) : SignatureStrategy {

    override suspend fun validate(call: ApplicationCall, rawBody: ByteArray): SignatureResult {
        val received = call.request.headers[headerName]
            ?: return SignatureResult.Invalid("Missing $headerName header")
        val expected = computeHmac(rawBody)
        return if (MessageDigest.isEqual(expected.toByteArray(), received.toByteArray()))
            SignatureResult.Valid
        else
            SignatureResult.Invalid("HMAC signature mismatch")
    }

    private fun computeHmac(body: ByteArray): String {
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secret.toByteArray(), algorithm))
        return prefix + mac.doFinal(body).joinToString("") { "%02x".format(it) }
    }
}

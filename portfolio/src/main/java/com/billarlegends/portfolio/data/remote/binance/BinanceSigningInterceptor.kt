package com.billarlegends.portfolio.data.remote.binance

import okhttp3.Interceptor
import okhttp3.Response
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Firma las peticiones marcadas con el header interno [SIGN_HEADER] siguiendo el esquema
 * HMAC-SHA256 que exige Binance para endpoints privados. El header se retira antes de
 * enviar la petición real; nunca viaja a Binance.
 *
 * La API key/secret solo existen en memoria mientras dura la sincronización: se leen desde
 * [com.billarlegends.portfolio.security.SecureCredentialStore] justo antes de crear este
 * interceptor y no se persisten en ningún otro sitio.
 */
class BinanceSigningInterceptor(
    private val apiKey: String,
    private val apiSecret: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val needsSigning = original.header(SIGN_HEADER) != null

        if (!needsSigning) {
            return chain.proceed(original)
        }

        val urlWithTimestamp = original.url.newBuilder()
            .addQueryParameter("timestamp", System.currentTimeMillis().toString())
            .addQueryParameter("recvWindow", "10000")
            .build()

        val signature = sign(urlWithTimestamp.encodedQuery.orEmpty(), apiSecret)

        val signedUrl = urlWithTimestamp.newBuilder()
            .addQueryParameter("signature", signature)
            .build()

        val signedRequest = original.newBuilder()
            .url(signedUrl)
            .removeHeader(SIGN_HEADER)
            .addHeader("X-MBX-APIKEY", apiKey)
            .build()

        return chain.proceed(signedRequest)
    }

    private fun sign(payload: String, secret: String): String {
        try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
            return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("HMAC-SHA256 no disponible", e)
        } catch (e: InvalidKeyException) {
            throw IllegalStateException("API secret inválido", e)
        }
    }

    companion object {
        const val SIGN_HEADER = "X-Portfolio-Needs-Signature"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }
}

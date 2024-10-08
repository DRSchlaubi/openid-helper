@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package dev.schlaubi.openid.helper.util

import dev.schlaubi.openid.helper.httpClient
import io.ktor.http.HeaderValueParam
import io.ktor.http.HttpMethod
import io.ktor.http.ParametersBuilder
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.OAuthCallback
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.signatureBaseStringInternal
import io.ktor.server.auth.simpleOAuth1aStep1
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun OAuthServerSettings.OAuth1aServerSettings.copy(
    consumerKey: String = this.consumerKey,
    consumerSecret: String = this.consumerSecret
) = OAuthServerSettings.OAuth1aServerSettings(
    name,
    requestTokenUrl,
    authorizeUrl,
    accessTokenUrl,
    consumerKey,
    consumerSecret
)

suspend fun ApplicationCall.requestToken(
    settings: OAuthServerSettings.OAuth1aServerSettings,
    callbackUrl: String,
    nonce: String
): OAuthCallback.TokenPair {
    return simpleOAuth1aStep1(
        httpClient,
        settings,
        callbackUrl,
        nonce
    )
}

fun ParametersBuilder.sign(baseUrl: String, secret: String, parameterName: String = HttpAuthHeader.Parameters.OAuthSignature, method: HttpMethod = HttpMethod.Post) {
    val signature = signatureBaseStringInternal(
        HttpAuthHeader.Parameterized("Oauth", emptyList()),
        method,
        baseUrl,
        entries().map { HeaderValueParam(it.key, it.value.first()) },
    ).hmacSha1(secret)

    set(parameterName, signature)
}

@OptIn(ExperimentalEncodingApi::class)
private fun String.hmacSha1(key: String): String {
    val keySpec = SecretKeySpec(key.toByteArray(), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(keySpec)

    return Base64.Default.encode(mac.doFinal(this.toByteArray()))
}

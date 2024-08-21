package dev.schlaubi.openid.helper.providers.implementations.mastodon

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import dev.schlaubi.openid.helper.Config
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

data class Token(val url: String, val token: String)

fun newMappedToken(url: String, mastodonAuthCode: String): String =
    newKey(url) {
        withClaim("token", mastodonAuthCode)
    }

private val verifier = JWT
    .require(Algorithm.HMAC256(Config.JWT_SECRET))
    .build()

fun verifyToken(token: String): Token {
    val decoded = verifier.verify(token)
    return Token(decoded.issuer, decoded.claims["token"]!!.asString())
}

private inline fun newKey(url: String, block: JWTCreator.Builder.() -> Unit) = JWT.create()
    .withIssuer(url)
    .apply(block)
    .withExpiresAt(Instant.ofEpochMilli(System.currentTimeMillis() + 2.minutes.inWholeMilliseconds))
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

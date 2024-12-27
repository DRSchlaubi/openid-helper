@file:OptIn(ExperimentalEncodingApi::class)

package dev.schlaubi.openid.helper.providers.implementations

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.kord.cache.api.put
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.providers.ProviderBuilder
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.State
import dev.schlaubi.openid.helper.util.cache
import dev.schlaubi.openid.helper.util.findAndRemoveState
import dev.schlaubi.openid.helper.util.registerState
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.util.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class PKCEState(override val id: String, val verifier: String, val redirectUri: String) : State

private fun newKey(verifier: String, token: String) = JWT
    .create()
    .withClaim("token", token)
    .withClaim("verifier", verifier)
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

private val verifier = JWT.require(Algorithm.HMAC256(Config.JWT_SECRET)).build()

fun ProviderRegistry.oauth2PKCE(
    name: String,
    authorizeUrl: String,
    tokenUrl: String,
    additional: ProviderBuilder.() -> Unit = {}
) = registerProvider(name = name) {
    onStartup {
        registerState<PKCEState>()
    }

    authorize {
        takeFrom(authorizeUrl)
        val verifier = generateVerifier()
        val redirectUri = parameters["redirect_uri"] ?: throw BadRequestException("Missing redirect_uri")
        val state = parameters["state"] ?: throw BadRequestException("Missing state")
        cache.put(PKCEState(state, verifier, redirectUri))
        parameters.append("code_challenge", generateCodeChallenge(verifier))
        parameters.append("code_challenge_method", "S256")
        parameters["redirect_uri"] = it.application.fullHref(ProviderRoute.Callback(name))
    }

    token {
        request {
            url(tokenUrl)

            formBody { (data, call) ->
                val code = data["code"] ?: throw BadRequestException("Missing code")
                val jwt = verifier.verify(code)
                val authCode = jwt.getClaim("token").asString()
                val verifier = jwt.getClaim("verifier").asString()

                set("code", authCode)
                set("code_verifier", verifier)
                set("redirect_uri", call.application.fullHref(ProviderRoute.Callback(name)))
                remove("client_secret")
            }
        }
    }

    routing {
        get<ProviderRoute.Callback> {
            val state = call.parameters["state"] ?: throw BadRequestException("Missing state")
            val code = call.parameters["code"] ?: throw BadRequestException("Missing code")
            val (_, verifier, redirectUri) = findAndRemoveState<PKCEState>(state) ?: throw BadRequestException("Missing state")

            val key = newKey(verifier, code)

            val redirect = url {
                takeFrom(redirectUri)

                parameters.append("code", key)
                parameters.append("state", state)
            }

            call.respondRedirect(redirect)
        }
    }

    additional()
}

fun generateVerifier() = Base64.UrlSafe.encode(generateNonce(32)).trimEnd('=')

fun generateCodeChallenge(verifier: String): String {
    val bytes = verifier.toByteArray(Charsets.US_ASCII)
    val digest = MessageDigest.getInstance("SHA-256").apply {
        update(bytes)
    }
    return Base64.UrlSafe.encode(digest.digest()).trimEnd('=')
}

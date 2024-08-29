package dev.schlaubi.openid.helper.providers.implementations

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.providers.ProviderBuilder
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.http.takeFrom
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.resources.get
import io.ktor.server.response.respondRedirect
import io.ktor.server.util.url
import io.ktor.util.generateNonce
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private data class PKCEState(val verifier: String, val redirectUri: String)

private val states = mutableMapOf<String, PKCEState>()

private fun newKey(verifier: String, token: String) = JWT
    .create()
    .withClaim("token", token)
    .withClaim("verifier", verifier)
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

private val verifier = JWT.require(Algorithm.HMAC256(Config.JWT_SECRET)).build()

@OptIn(ExperimentalEncodingApi::class)
fun ProviderRegistry.oauth2PKCE(
    name: String,
    authorizeUrl: String,
    tokenUrl: String,
    additional: ProviderBuilder.() -> Unit = {}
) = registerProvider(name = name) {
    authorize {
        takeFrom(authorizeUrl)
        val verifier = Base64.UrlSafe.encode(generateNonce(32)).trimEnd('=')
        val redirectUri = parameters["redirect_uri"] ?: throw BadRequestException("Missing redirect_uri")
        states[parameters["state"] ?: throw BadRequestException("Missing state")] = PKCEState(verifier, redirectUri)
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
            val (verifier, redirectUri) = states.remove(state) ?: throw BadRequestException("Missing state")

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

@OptIn(ExperimentalEncodingApi::class)
private fun generateCodeChallenge(verifier: String): String {
    val bytes = verifier.toByteArray(Charsets.US_ASCII)
    val digest = MessageDigest.getInstance("SHA-256").apply {
        update(bytes)
    }
    return Base64.UrlSafe.encode(digest.digest()).trimEnd('=')
//    return java.util.Base64.getEncoder().withoutPadding().encodeToString(digest.digest())
}

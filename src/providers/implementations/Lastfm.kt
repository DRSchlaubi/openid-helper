package dev.schlaubi.openid.helper.providers.implementations

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.kord.cache.api.put
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.State
import dev.schlaubi.openid.helper.util.cache
import dev.schlaubi.openid.helper.util.findAndRemoveState
import dev.schlaubi.openid.helper.util.md5
import dev.schlaubi.openid.helper.util.registerState
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.request
import io.ktor.http.ParametersBuilder
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.encodeURLParameter
import io.ktor.http.takeFrom
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.resources.get
import io.ktor.server.response.respondRedirect
import io.ktor.util.date.GMTDate
import io.ktor.util.date.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

@Serializable
data class LastfmState(override val id: String, val redirectUrl: String) : State

private fun newKey(sub: String, apiKey: String) = JWT
    .create()
    .withClaim("sub", sub)
    .withClaim("api_key", apiKey)
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

private val validator = JWT
    .require(Algorithm.HMAC256(Config.JWT_SECRET))
    .build()

@OptIn(ExperimentalStdlibApi::class)
fun ProviderRegistry.lastfm() = registerProvider("lastfm") {
    onStartup {
        registerState<LastfmState>()
    }
    authorize {
        if (it.parameters["response_type"] != "code") {
            throw BadRequestException("Response type must be code")
        }
        val state = it.parameters["state"] ?: throw BadRequestException("State is missing")
        val redirectUri = it.parameters["redirect_uri"] ?: throw BadRequestException("Redirect uri is missing")
        val clientId = it.parameters["client_id"] ?: throw BadRequestException("Client id is missing")
        cache.put(LastfmState(state, redirectUri))

        it.response.cookies.append("state", state, path = "/providers/lastfm", expires = GMTDate() + 2.minutes)

        takeFrom("https://www.last.fm/api/auth")
        parameters.apply {
            clear()
            append("api_key", clientId)
            append("cb", it.application.fullHref(ProviderRoute.Callback(name)))
        }
    }

    token {
        request {
            url("https://ws.audioscrobbler.com/2.0/")

            formBody { (body, _, request) ->
                if (body["grant_type"] != "authorization_code") {
                    throw BadRequestException("Grant type must be authorization_code")
                }
                val token = body["code"] ?: throw BadRequestException("Code is missing")
                val clientId = body["client_id"] ?: throw BadRequestException("Client id is missing")
                val clientSecret = body["client_secret"] ?: throw BadRequestException("Client secret is missing")

                request.url.parameters.append("format", "json")
                append("token", token)

                // https://www.last.fm/api/webauth#_6-sign-your-calls
                sign("auth.getSession", clientSecret, clientId)
            }
        }

        response {
            json { (data, response) ->
                val session = data["session"]!!.jsonObject
                val apiKey = (response.request.content as FormDataContent).formData["api_key"]!!
                put("access_token", newKey(session["name"]!!.jsonPrimitive.content, apiKey))
                put("token_type", "bearer")
            }
        }
    }

    userEndpoint {
        request {
            url("https://ws.audioscrobbler.com/2.0/")

            formBody { (_, call, request) ->
                val authorization = (call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob
                    ?: throw BadRequestException("Authorization header is missing")
                val token = validator.verify(authorization)

                request.url.parameters.apply {
                    append("format", "json")
                    append("method", "user.getInfo")
                    append("api_key", token.claims["api_key"]!!.asString())
                    append("user", token.claims["sub"]!!.asString())
                }
            }
        }

        response {
            json { (data) ->
                val user = data["user"]!!.jsonObject
                put("sub", user["name"]!!)
                put("preferred_username", user["name"]!!)
            }
        }
    }

    routing {
        get<ProviderRoute.Callback> {
            val stateName = call.request.cookies["state"]
            val state = findAndRemoveState<LastfmState>(stateName) ?: throw BadRequestException("State is missing")
            val token = call.parameters["token"] ?: throw BadRequestException("Token is missing")

            call.respondRedirect {
                takeFrom(state.redirectUrl)
                parameters.apply {
                    clear()
                    append("code", token)
                    append("state", stateName!!)
                }
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun ParametersBuilder.sign(method: String, clientSecret: String, clientId: String) {
    append("api_key", clientId)
    append("method", method)
    val signatureBase = buildString {
        entries()
            .sortedBy(Map.Entry<String, List<String>>::key)
            .forEach { (key, values) ->
                val value = values.single()

                append(key)
                append(value)
            }
        append(clientSecret.encodeURLParameter())
    }

    append("api_sig", signatureBase.md5())
}

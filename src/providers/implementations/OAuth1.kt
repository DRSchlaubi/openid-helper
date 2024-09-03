package dev.schlaubi.openid.helper.providers.implementations

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.providers.ProviderBuilder
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.URLUpdater
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.copy
import dev.schlaubi.openid.helper.util.requestToken
import dev.schlaubi.openid.helper.util.sign
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.parseUrlEncodedParameters
import io.ktor.http.takeFrom
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.resources.get
import io.ktor.server.response.respondRedirect
import io.ktor.server.util.url
import io.ktor.util.date.GMTDate
import io.ktor.util.date.plus
import io.ktor.util.generateNonce
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

data class AccessToken(val token: String, val tokenSecret: String)

private data class State(val token: String, val tokenSecret: String, val redirectUri: String)

private val states = mutableMapOf<String, State>()

private fun newJWT(
    issuer: String,
    token: String,
    tokenSecret: String,
    additional: JWTCreator.Builder.() -> Unit = {}
) = JWT.create()
    .withIssuer(issuer)
    .withClaim("token", token)
    .withClaim("token_secret", tokenSecret)
    .apply(additional)
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

private fun newAuthCode(
    issuer: String,
    token: String,
    verifier: String,
    tokenSecret: String
) = newJWT(issuer, token, tokenSecret) {
    withClaim("verifier", verifier)
}

private val verifier = JWT
    .require(Algorithm.HMAC256(Config.JWT_SECRET))
    .build()

fun ProviderRegistry.oauth1a(
    config: OAuthServerSettings.OAuth1aServerSettings,
    additionalAuthParams: URLUpdater = {},
    additional: ProviderBuilder.(OAuthServerSettings.OAuth1aServerSettings) -> Unit = {}
) =
    registerProvider(config.name) {
        authorize {
            takeFrom(config.authorizeUrl)

            val state = parameters["state"] ?: throw BadRequestException("Missing state")
            val clientId = parameters["client_id"] ?: throw BadRequestException("Missing client_id")
            val redirectUri = parameters["redirect_uri"] ?: throw BadRequestException("Missing redirect_uri")

            val settings = config.copy(consumerKey = clientId)

            val (token, tokenSecret) = it.requestToken(
                settings,
                it.application.fullHref(ProviderRoute.Callback(config.name)),
                state
            )
            states[state] = State(token, tokenSecret, redirectUri)

            it.response.cookies.append(
                "state",
                state,
                path = "/",
                expires = GMTDate() + 2.minutes
            )

            parameters.apply {
                clear()
                append("oauth_token", token)
            }
            additionalAuthParams(this, it)
        }

        token {
            request {
                url(config.accessTokenUrl)

                formBody { (data, _, _) ->
                    if (data["grant_type"] != "authorization_code") {
                        throw BadRequestException("Grant type must be authorization_code")
                    }
                    val code = data["code"] ?: throw BadRequestException("Missing code")
                    val clientId = data["client_id"] ?: throw BadRequestException("Missing client_id")
                    val clientSecret = data["client_secret"] ?: throw BadRequestException("Missing client_secret")

                    val verifiedCode = verifier.verify(code)
                    val verifier = verifiedCode.getClaim("verifier").asString()
                    val token = verifiedCode.getClaim("token").asString()
                    val tokenSecret = verifiedCode.getClaim("token_secret").asString()

                    clear()

                    set(HttpAuthHeader.Parameters.OAuthNonce, generateNonce())
                    set(HttpAuthHeader.Parameters.OAuthTimestamp, Instant.now().epochSecond.toString())
                    set(HttpAuthHeader.Parameters.OAuthVerifier, verifier)
                    set(HttpAuthHeader.Parameters.OAuthConsumerKey, clientId)
                    set(HttpAuthHeader.Parameters.OAuthSignatureMethod, "HMAC-SHA1")
                    set(HttpAuthHeader.Parameters.OAuthVersion, "1.0")
                    set(HttpAuthHeader.Parameters.OAuthToken, token)

                    sign(config.accessTokenUrl, "$clientSecret&$tokenSecret")
                }
            }

            response {
                plainText<JsonObject> { (data, _) ->
                    val parameters = data.readUTF8Line()!!.parseUrlEncodedParameters()

                    val token = parameters["oauth_token"]!!
                    val tokenSecret = parameters["oauth_token_secret"]!!

                    buildJsonObject {
                        put("access_token", newJWT(config.name, token, tokenSecret))
                        put("token_type", "bearer")
                    }
                }
            }
        }

        routing {
            get<ProviderRoute.Callback> {
                val stateId = call.request.cookies["state"]
                val state = states.remove(stateId) ?: throw BadRequestException("Missing state")
                val oauthToken = call.parameters["oauth_token"] ?: throw BadRequestException("Missing oauth_token")
                val oauthVerifier =
                    call.parameters["oauth_verifier"] ?: throw BadRequestException("Missing oauth_verifier")

                val key = newAuthCode(config.name, oauthToken, oauthVerifier, state.tokenSecret)

                val redirectTo = url {
                    takeFrom(state.redirectUri)
                    parameters.append("code", key)
                    parameters.append("state", stateId.toString())
                }

                // Delete cookie
                call.response.cookies.append("state", value = "", expires = GMTDate())
                call.respondRedirect(redirectTo)
            }
        }

        additional(this, config)
    }

fun ApplicationCall.verifyToken(): AccessToken {
    val token =
        request.parseAuthorizationHeader() as? HttpAuthHeader.Single ?: throw BadRequestException("Missing token")

    val verifiedToken = verifier.verify(token.blob.toString())

    return AccessToken(verifiedToken.getClaim("token").asString(), verifiedToken.getClaim("token_secret").asString())
}

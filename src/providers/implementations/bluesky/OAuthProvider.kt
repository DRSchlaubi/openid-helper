package dev.schlaubi.openid.helper.providers.implementations.bluesky

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.kord.cache.api.put
import dev.kord.cache.api.remove
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@Serializable
data class DPoPKey(
    override val id: String,
    val key: SerializableECKey,
    val did: String,
    val authorizationServer: AuthorizationServerInfo,
    val token: String
) : State

private fun newAuthCode(token: PARToken, dpopKid: String) = JWT.create()
    .withSubject(token.sub)
    .withClaim("dpop-kid", dpopKid)
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

private fun newAccessToken(dpopKid: String) = JWT.create()
    .withSubject(dpopKid)
    .withExpiresAt(Instant.now().plus(Duration.ofMinutes(1)))
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

val authCodeVerifier = JWT.require(Algorithm.HMAC256(Config.JWT_SECRET)).build()

fun ApplicationCall.verifyRequest() {
    if (parameters["client_id"] != Config.BLUESKY_CLIENT_ID) throw BadRequestException("Missing client_id")
    if (parameters["redirect_uri"] !in Config.BLUESKY_REDIRECT_URIS) throw BadRequestException("invalid redirect uri")
}

fun ProviderRegistry.bluesky() = registerProvider("bluesky") {
    onStartup {
        registerState<DPoPKey>()
        registerState<PARAuthData>()
    }

    authorize {
        it.verifyRequest()
        val state = it.parameters["state"] ?: throw BadRequestException("Missing state")
        val scope = it.parameters["scope"] ?: throw BadRequestException("Missing scope")
        val clientId = it.parameters["client_id"] ?: throw BadRequestException("Missing client_id")
        val redirectUri = it.parameters["redirect_uri"] ?: throw BadRequestException("Missing redirect_uri")

        parameters.clear()
        takeFrom(fullHref(BlueSkyRoute.InitiateLogin(state, scope, clientId, redirectUri)))
    }

    userEndpoint {
        request {
            json { (_, call, request) ->
                val token = (call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob
                    ?: throw BadRequestException("Invalid token")
                val parsed = authCodeVerifier.verify(token)
                val kid = findAndRemoveState<DPoPKey>(parsed.subject) ?: throw BadRequestException("Invalid token")

                request.url {
                    takeFrom(kid.authorizationServer.resource)
                    path("xrpc", "app.bsky.actor.getProfile")
                    parameters.append("actor", kid.did)
                }
                request.headers[HttpHeaders.Authorization] = "DPoP ${kid.token}"
                request.signWithDPoPAuthenticated(kid.key, null, kid.authorizationServer.resource, kid.token)
            }
        }

        response {
            json {(data) ->
                put("sub", data["did"]!!)
                put("preferred_username", data["handle"]!!)
                data["displayName"]?.let { put("name", it) }
                data["picture"]?.let { put("avatar", it) }
            }
        }
    }

    routing {
        blueskyRoute()
        blueskyOAuth()
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun Route.blueskyOAuth() {
    get<BlueSkyRoute.Callback> { (state, code, _) ->
        val authData = findAndRemoveState<PARAuthData>(state) ?: throw BadRequestException("Missing state")

        val response = requestToken(authData, code)
        if (authData.expectedDid != null && authData.expectedDid != response.sub) throw BadRequestException("Invalid did")
        cache.put(DPoPKey(authData.ecKey.keyID, authData.ecKey, response.sub, authData.authorizationServer, response.accessToken))
        GlobalScope.launch {
            delay(5.minutes)
            cache.remove<DPoPKey> { DPoPKey::id eq authData.ecKey.keyID }
        }

        call.respondRedirect {
            takeFrom(authData.redirectUri)
            parameters.append("state", state)
            parameters.append("code", newAuthCode(response, authData.ecKey.keyID))
        }
    }

    post<ProviderRoute.Token> {
        val parameters = call.receiveParameters()
        val code = parameters["code"] ?: throw BadRequestException("Missing code")
        if (parameters["grant_type"] != "authorization_code") throw BadRequestException("Missing grant_type")
        if (parameters["client_id"] != Config.BLUESKY_CLIENT_ID) throw BadRequestException("Invalid client_id")
        if (parameters["client_secret"] != Config.BLUESKY_CLIENT_SECRET) throw BadRequestException("Invalid client_secret")

        val authCode = authCodeVerifier.verify(code)

        val accessToken = newAccessToken(authCode.getClaim("dpop-kid").asString())

        val response = buildJsonObject {
            put("token_type", "Bearer")
            put("access_token", accessToken)
            put("expires_in", 60)
        }

        call.respond(response)
    }
}

package dev.schlaubi.openid.helper.providers.implementations.bluesky

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.httpClient
import dev.schlaubi.openid.helper.providers.implementations.generateCodeChallenge
import dev.schlaubi.openid.helper.providers.implementations.generateVerifier
import dev.schlaubi.openid.helper.util.SerializableECKey
import dev.schlaubi.openid.helper.util.State
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PARResponse(
    @SerialName("request_uri")
    val requestUri: String,
    @SerialName("expires_in")
    val expiresIn: Long,
)

@Serializable
data class PARToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    val scope: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    val sub: String
)

@Serializable
data class PARAuthData(
    override val id: String,
    val expectedDid: String?,
    val requestUri: String,
    val dPoPNonce: String,
    val pkceVerifier: String,
    val authorizationServer: AuthorizationServerInfo,
    val ecKey: SerializableECKey,
    val redirectUri: String
) : State

private fun newClientAssertion(pdsUrl: String) = JWT.create()
    .withKeyId(dpopKeyId)
    .withIssuer(blueSkyClientId)
    .withSubject(blueSkyClientId)
    .withAudience(pdsUrl)
    .withJWTId(generateNonce())
    .withIssuedAt(Instant.now())
    .sign(Algorithm.ECDSA256(Config.PRIVATE_DPOP_SIGNING_KEY))

private fun ParametersBuilder.addClientAssertion(authorizationServer: OAuthAuthorizationServer) {
    set("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
    set("client_assertion", newClientAssertion(authorizationServer.issuer))
}

suspend fun requestPAR(info: LoginInformation, scope: String, state: String, redirectUri: String): PARAuthData {
    val verifier = generateVerifier()
    val body = Parameters.build {
        set("response_type", "code")
        set("client_id", blueSkyClientId)
        set("redirect_uri", fullHref(BlueSkyRoute.CallbackRoute()))
        set("code_challenge_method", "S256")
        set("code_challenge", generateCodeChallenge(verifier))
        set("scope", scope)
        set("state", state)
        if (info.loginHint != null) {
            set("login_hint", info.loginHint)
        }
        addClientAssertion(info.authServer.server)
    }
    val dPoPKeyPair = newDPoPKeyPair()
    val response = httpClient.submitForm(info.authServer.server.pushedAuthorizationRequestEndpoint, body) {
        signWithDPoP(dPoPKeyPair, null)
    }
    val dpopNonce = response.headers["DPoP-Nonce"]!!
    val par = response.body<PARResponse>()
    val parAuthData = PARAuthData(state, info.did, par.requestUri, dpopNonce, verifier, info.authServer, dPoPKeyPair, redirectUri)

    return parAuthData
}

suspend fun requestToken(authData: PARAuthData, code: String): PARToken {
    val parameters = Parameters.build {
        set("grant_type", "authorization_code")
        set("client_id", blueSkyClientId)
        set("code_verifier", authData.pkceVerifier)
        set("code", code)
        set("redirect_uri", fullHref(BlueSkyRoute.CallbackRoute()))
        addClientAssertion(authData.authorizationServer.server)
    }
    val response = httpClient.submitForm(authData.authorizationServer.server.tokenEndpoint, parameters) {
        signWithDPoP(authData.ecKey, null)
    }.body<PARToken>()

    return response
}

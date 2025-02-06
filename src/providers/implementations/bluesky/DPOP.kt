package dev.schlaubi.openid.helper.providers.implementations.bluesky

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.providers.implementations.generateCodeChallenge
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.Instant

val blueSkyClientId = fullHref(BlueSkyRoute.ClientMetadata())
private val generator = ECKeyGenerator(Curve.P_256)
fun newDPoPKeyPair(): ECKey = ECKey.Builder(generator.generate())
    .keyIDFromThumbprint()
    .keyUse(KeyUse.SIGNATURE)
    .build()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DPoPError(
    val error: String,
    @JsonNames("error_description", "message")
    val errorDescription: String
)

fun HttpRequestBuilder.signWithDPoP(
    signingKey: ECKey,
    nonce: String?,
    additional: JWTCreator.Builder.() -> Unit = {}
) {
    val now = Instant.now()
    fun createToken(nonce: String?) = JWT.create()
        .withHeader(mapOf("typ" to "dpop+jwt", "jwk" to signingKey.toPublicJWK().toJSONObject()))
        .withJWTId(generateNonce())
        .withClaim("htm", method.value)
        .withClaim("htu", url.buildString())
        .withIssuedAt(now)
        .withExpiresAt(now.plusSeconds(60))
        .apply(additional)
        .apply {
            if (nonce != null) {
                withClaim("nonce", nonce)
            }
        }
        .sign(Algorithm.ECDSA256(signingKey.toECPrivateKey()))
    expectSuccess = false

    retry {
        maxRetries = 1

        retryIf { _, httpResponse ->
            val dpopNonce = httpResponse.headers["DPoP-Nonce"]
            if ((httpResponse.status != HttpStatusCode.BadRequest && httpResponse.status != HttpStatusCode.Unauthorized) || dpopNonce == null) {
                false
            } else {
                val error = runBlocking {
                    withTimeoutOrNull(100) { httpResponse.body<DPoPError>() }
                }
                error?.errorDescription == "use_dpop_nonce"
            }
        }

        modifyRequest {
            val newNonce = response?.headers?.get("DPoP-Nonce") ?: return@modifyRequest
            it.headers["DPoP"] = createToken(newNonce)
        }
    }

    headers["DPoP"] = createToken(nonce)
}

fun HttpRequestBuilder.signWithDPoPAuthenticated(ecKey: ECKey, nonce: String?, pdsServer: String, token: String) =
    signWithDPoP(ecKey, nonce) {
        withAudience("aud", pdsServer)
        withClaim("ath", generateCodeChallenge(token))
    }

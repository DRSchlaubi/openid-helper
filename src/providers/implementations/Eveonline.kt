package dev.schlaubi.openid.helper.providers.implementations

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.blocking
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.net.URI

suspend fun ProviderRegistry.eveonline() = registerProvider("eveonline") {
    val jwks = blocking {
        JWKSet.load(URI.create("https://login.eveonline.com/oauth/jwks").toURL())
    }

    authorize("https://login.eveonline.com/v2/oauth/authorize")
    token("https://login.eveonline.com/v2/oauth/token")

    routing {
        get<ProviderRoute.UserInfo> {
            val token = (call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob
                ?: throw BadRequestException("Invalid token")

            val jwt = SignedJWT.parse(token)
            val keyId = jwt.header.keyID ?: throw BadRequestException("Missing key id")
            val key = blocking { jwks.getKeyByKeyId(keyId) }

            val algorithm = when (key.algorithm) {
                JWSAlgorithm.RS256 -> RSASSAVerifier(key as RSAKey)
                JWSAlgorithm.ES256 -> ECDSAVerifier(key as ECKey)
                else -> throw UnsupportedOperationException("Unsupported algorithm: ${key.algorithm}")
            }
            require(jwt.verify(algorithm)) { "Invalid signature" }

            val response = buildJsonObject {
                jwt.jwtClaimsSet.claims.forEach { (key, value) ->
                    val primitive = (value as? Long)?.let(::JsonPrimitive)
                        ?: JsonPrimitive(value.toString())

                    put(key, primitive)
                }
            }

            call.respond(response)
        }
    }
}

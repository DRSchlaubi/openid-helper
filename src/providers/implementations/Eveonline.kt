package dev.schlaubi.openid.helper.providers.implementations

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.blocking
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.net.URI
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey


suspend fun ProviderRegistry.eveonline() = registerProvider("eveonline") {
    val jwks = blocking {
        JwkProviderBuilder(URI.create("https://login.eveonline.com/oauth/jwks").toURL())
            .cached(true)
            .build()
    }

    authorize("https://login.eveonline.com/v2/oauth/authorize")
    token("https://login.eveonline.com/v2/oauth/token")

    routing {
        get<ProviderRoute.UserInfo> {
            val token = (call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob
                ?: throw BadRequestException("Invalid token")

            val jwt = JWT.decode(token)
            val keyId = jwt.keyId
            val key = blocking { jwks.get(keyId) }

            val algorithm = when (key.algorithm) {
                "RS256" -> Algorithm.RSA256(key.publicKey as RSAPublicKey)
                "ES256" -> Algorithm.ECDSA256(key.publicKey as ECPublicKey)
                else -> throw UnsupportedOperationException("Unsupported algorithm: ${key.algorithm}")
            }

            val verifier = JWT.require(algorithm).build()
            val validatedJwt = verifier.verify(jwt)

            val response = buildJsonObject {
                validatedJwt.claims.forEach { (key, value) ->
                    val primitive = value.asLong()?.let(::JsonPrimitive) ?: JsonPrimitive(value.asString())

                    put(key, primitive)
                }
            }

            call.respond(response)
        }
    }
}

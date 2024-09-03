package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.util.sign
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.util.*
import java.time.Instant

private val identify = "https://api.discogs.com/oauth/identity"

fun ProviderRegistry.discogs() = oauth1a(
    OAuthServerSettings.OAuth1aServerSettings(
        "discogs",
        "https://api.discogs.com/oauth/request_token",
        "https://discogs.com/oauth/authorize",
        "https://api.discogs.com/oauth/access_token",
        Config.DISCOGS_CONSUMER_KEY,
        Config.DISCOGS_CONSUMER_SECRET
    )
) {
    userEndpoint {
        request {
            url("")
            url {
                val (token, tokenSecret) = it.verifyToken()
                takeFrom(identify)

                parameters.apply {
                    set(HttpAuthHeader.Parameters.OAuthNonce, generateNonce())
                    set(HttpAuthHeader.Parameters.OAuthTimestamp, Instant.now().epochSecond.toString())
                    set(HttpAuthHeader.Parameters.OAuthConsumerKey, Config.DISCOGS_CONSUMER_KEY)
                    set(HttpAuthHeader.Parameters.OAuthSignatureMethod, "HMAC-SHA1")
                    set(HttpAuthHeader.Parameters.OAuthVersion, "1.0")
                    set(HttpAuthHeader.Parameters.OAuthToken, token)

                    sign(
                        identify,
                        "${Config.DISCOGS_CONSUMER_SECRET}&${tokenSecret}",
                        method = HttpMethod.Get
                    )
                }
            }
        }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}

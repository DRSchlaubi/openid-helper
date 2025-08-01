package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.util.md5
import io.ktor.http.ParametersBuilder
import io.ktor.http.takeFrom
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.flickr() = oauth1a(
    OAuthServerSettings.OAuth1aServerSettings(
        "flickr",
        requestTokenUrl = "https://www.flickr.com/services/oauth/request_token",
        authorizeUrl = "https://www.flickr.com/services/oauth/authorize",
        accessTokenUrl = "https://www.flickr.com/services/oauth/access_token",
        consumerKey = Config.FLICKR_CONSUMER_KEY,
        consumerSecret = Config.FLICKR_CONSUMER_SECRET
    ), {
        parameters.append("perms", it.parameters["scope"] ?: throw BadRequestException("Missing scope"))
    }) { config ->
    userEndpoint {
        request {
            url("https://www.flickr.com/services/rest")

            url { call ->
                val (token, _) = call.verifyToken()

                takeFrom("https://www.flickr.com/services/rest")

                parameters.apply {
                    set("method", "flickr.auth.oauth.checkToken")
                    set("oauth_token", token)
                    set("api_key", config.consumerKey)
                    set("format", "json")
                    set("nojsoncallback", "1")

                    sign(config.consumerSecret)
                }
            }
        }

        response {
            json { (data) ->
                val oauth = data["oauth"]!!.jsonObject
                val user = oauth["user"]!!.jsonObject

                put("sub", user["nsid"]!!)
                put("preferred_username", user["username"]!!)
            }
        }
    }
}

// https://github.com/boncey/Flickr4Java/blob/master/src/main/java/com/flickr4java/flickr/auth/AuthInterface.java#L269
private fun ParametersBuilder.sign(sharedSecret: String) {
    val signature = buildString {
        append(sharedSecret)
        entries().sortedBy(Map.Entry<String, List<String>>::key).forEach { (key, value) ->
            append(key)
            append(value.first())
        }
    }.md5()

    set("api_sig", signature)
}

package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import kotlinx.serialization.json.put

fun ProviderRegistry.mixcloud() = registerProvider("mixcloud") {
    authorize("https://www.mixcloud.com/oauth/authorize")
    token {
        request {
            url("https://www.mixcloud.com/oauth/access_token")
        }

        response {
            json { (_) ->
                put("token_type", "Bearer")
            }
        }
    }

    userEndpoint {
        request {
            url {
                takeFrom("https://api.mixcloud.com/me")
                parameters.append(
                    "access_token",
                    (it.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob.toString()
                )
            }
        }

        response {
            json { (data) ->
                put("sub", data["username"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}
package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.takeFrom
import io.ktor.server.auth.parseAuthorizationHeader
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

fun ProviderRegistry.vk() = registerProvider("vk") {
    authorize("https://oauth.vk.com/authorize")
    token {
        request {
            url("https://oauth.vk.com/access_token")
        }
        response {
            json {
                put("token_type", "bearer")
            }
        }
    }

    userEndpoint {
        request {
            url {
                val header = it.request.parseAuthorizationHeader() as HttpAuthHeader.Single
                takeFrom("https://api.vk.com/method/users.get")

                parameters.append("access_token", header.blob)
                parameters.append("v", "5.103")
            }
        }

        response {
            json {(data) ->
                val response = data["response"]!!.jsonArray.first().jsonObject
                put("sub", response["id"]!!)
            }
        }
    }
}
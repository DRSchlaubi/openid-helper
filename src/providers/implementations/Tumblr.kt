package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.bearerAuth
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.parseAuthorizationHeader
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.tumblr() = registerProvider("tumlbr") {
    authorize("https://www.tumblr.com/oauth2/authorize")
    token("https://api.tumblr.com/v2/oauth2/token")

    userEndpoint {
        request {
            url("https://api.tumblr.com/v2/user/info")
            json({ _, call ->
                bearerAuth((call.request.parseAuthorizationHeader() as HttpAuthHeader.Single).blob)
            }) {}
        }

        response {
            json {
                val response = it["response"]!!.jsonObject
                val user = response["user"]!!.jsonObject
                put("sub", user["name"]!!)
            }
        }
    }
}

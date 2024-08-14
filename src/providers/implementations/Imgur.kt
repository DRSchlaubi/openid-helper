package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.bearerAuth
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.parseAuthorizationHeader
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.imgur() = registerProvider("imgur") {
    authorize("https://api.imgur.com/oauth2/authorize")
    token("https://api.imgur.com/oauth2/token")
    userEndpoint {
        request {
            url("https://api.imgur.com/3/account/me")
            json({ _, call ->
                bearerAuth((call.request.parseAuthorizationHeader() as HttpAuthHeader.Single).blob)
            }) {}
        }

        response {
            json {
                val data = it["data"]!!.jsonObject
                put("sub", data["id"]!!)
            }
        }
    }
}

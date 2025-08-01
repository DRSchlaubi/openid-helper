package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.useHeaderForOAuthClientCredentials
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.rocketbeans() = registerProvider("rbtv") {
    authorize("https://rocketbeans.tv/oauth2/authorize")
    token("https://api.rocketbeans.tv/v1/oauth2/token")

    userEndpoint {
        request {
            url("https://api.rocketbeans.tv/v1/user/self")
        }

        response {
            json { (json) ->
                val data = json["data"]!!.jsonObject
                put("sub", data["id"]!!)
                put("preferred_username", data["displayName"]!!)
            }
        }
    }
}
package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.dailymotion() = registerProvider("dailymotion") {
    authorize("https://api.dailymotion.com/oauth/authorize")
    token("https://api.dailymotion.com/oauth/token")

    userEndpoint {
        request { url("https://api.dailymotion.com/auth") }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}
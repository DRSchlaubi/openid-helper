package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.deviantart() =registerProvider("deviantart") {
    authorize("https://www.deviantart.com/oauth2/authorize")
    token("https://www.deviantart.com/oauth2/token")

    userEndpoint {
        request { url("https://www.deviantart.com/api/v1/oauth2/user/whoami") }

        response {
            json { (data) ->
                put("sub", data["userid"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}
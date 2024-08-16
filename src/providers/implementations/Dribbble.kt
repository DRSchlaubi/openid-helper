package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.dribbble() = registerProvider("dribbble") {
    authorize("https://dribbble.com/oauth/authorize")
    token("https://dribbble.com/oauth/token")

    userEndpoint {
        request {
            url("https://api.dribbble.com/v2/user")
        }

        response {
            json {(data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["name"]!!)
            }
        }
    }
}

package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.strava() = registerProvider("strava") {
    authorize("https://www.strava.com/oauth/authorize")
    token("https://www.strava.com/api/v3/oauth/token")

    userEndpoint {
        request {
            url("https://www.strava.com/api/v3/athlete")
        }

        response {
            json {
                put("sub", it["id"]!!)
            }
        }
    }
}

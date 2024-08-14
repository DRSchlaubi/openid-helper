package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.figma() = registerProvider("figma") {
    authorize("https://www.figma.com/oauth")
    token("https://www.figma.com/api/oauth/token")

    userEndpoint {
        request {
            url("https://api.figma.com/v1/me")
        }

        response {
            json {
                put("sub", it["id"]!!)
            }
        }
    }
}
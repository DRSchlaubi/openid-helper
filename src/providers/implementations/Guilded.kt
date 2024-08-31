package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.guilded() = registerProvider("guilded") {
    authorize("https://authlink.app/auth")
    token("https://authlink.app/api/v1/token")

    userEndpoint {
        request { url("https://authlink.app/api/v1/users/@me") }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["name"]!!)
            }
        }
    }
}

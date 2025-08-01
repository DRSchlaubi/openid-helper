package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.osu() = registerProvider("osu") {
    authorize("https://osu.ppy.sh/oauth/authorize")
    token("https://osu.ppy.sh/oauth/token")

    userEndpoint {
        request { url("https://osu.ppy.sh/api/v2/me") }
        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}

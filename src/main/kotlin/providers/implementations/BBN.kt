package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.bbn() = registerProvider("bbn") {
    authorize("https://bbn.one/oauth")
    token("https://bbn.one/api/@bbn/oauth/token")

    userEndpoint {
        request { url("https://bbn.one/api/@bbn/oauth/userinfo") }
        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["name"]!!)
                put("email", data["email"]!!)
            }
        }
    }
}

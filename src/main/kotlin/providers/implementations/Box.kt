package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer

fun ProviderRegistry.box() = registerProvider("box") {
    authorize("https://account.box.com/api/oauth2/authorize")
    token("https://api.box.com/oauth2/token")

    userEndpoint {
        request {
            fixLowercaseBearer()
            url("https://api.box.com/2.0/users/me")
        }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["login"]!!)
            }
        }
    }
}

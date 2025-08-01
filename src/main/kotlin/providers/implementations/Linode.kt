package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer

fun ProviderRegistry.linode() =registerProvider("linode") {
    authorize("https://login.linode.com/oauth/authorize")
    token("https://login.linode.com/oauth/token")

    userEndpoint {
        request {
            fixLowercaseBearer()
            url("https://api.linode.com/v4/profile")
        }

        response {
            json { (data) ->
                put("sub", data["uid"]!!)
                put("preferred_username", data["username"]!!)
                put("email", data["email"]!!)
            }
        }
    }
}
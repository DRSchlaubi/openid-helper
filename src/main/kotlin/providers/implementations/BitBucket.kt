package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer

fun ProviderRegistry.bitbucket() = registerProvider("bitbucket") {
    authorize("https://bitbucket.org/site/oauth2/authorize")
    token("https://bitbucket.org/site/oauth2/access_token")

    userEndpoint {
        request {
            url("https://api.bitbucket.org/2.0/user")
            fixLowercaseBearer()
        }

        response {
            json { (data) ->
                put("sub", data["uuid"]!!)
                put("preferred_username", data["display_name"]!!)
            }
        }
    }
}

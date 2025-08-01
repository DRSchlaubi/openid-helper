package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.wordpressCom() = registerProvider("wordpress") {
    authorize("https://public-api.wordpress.com/oauth2/authorize")
    token("https://public-api.wordpress.com/oauth2/token")

    userEndpoint {
        request {
            url("https://public-api.wordpress.com/rest/v1/me/")
        }

        response {
            json { (data) ->
                put("sub", data["ID"]!!)
                put("preferred_username", data["username"]!!)
                put("email", data["email"]!!)
            }
        }
    }
}
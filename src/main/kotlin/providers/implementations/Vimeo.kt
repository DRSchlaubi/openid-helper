package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.vimeo() = registerProvider("vimeo") {
    authorize("https://api.vimeo.com/oauth/authorize")
    token("https://api.vimeo.com/oauth/access_token")

    userEndpoint {
        request {
            url("https://api.vimeo.com/me")
        }

        response {
            json { (data) ->
                put("sub", data["uri"]!!)
                data["link"]?.let { put("preferred_username", it) }
            }
        }
    }

}
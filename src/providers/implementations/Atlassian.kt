package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.atlassian() = registerProvider("atlassian") {
    authorize("https://auth.atlassian.com/authorize")
    token("https://auth.atlassian.com/oauth/token")

    userEndpoint {
        request {
            url("https://api.atlassian.com/me")
        }

        response {
            json {(data) ->
                put("sub", data["account_id"]!!)
                put("email", data["email"]!!)
                put("nickname", data["nickname"]!!)
            }
        }
    }
}

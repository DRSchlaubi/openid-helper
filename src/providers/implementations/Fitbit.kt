package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.useHeaderForOAuthClientCredentials
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.fitbit() = registerProvider("fitbit") {
    authorize("https://www.fitbit.com/oauth2/authorize")
    token {
        request {
            useHeaderForOAuthClientCredentials()
            url("https://api.fitbit.com/oauth2/token")
        }
    }

    userEndpoint {
        request {
            url("https://api.fitbit.com/1/user/-/profile.json")
        }

        response {
            json {
                val user = it["user"]!!.jsonObject
                put("sub", user["encodedId"]!!)
                put("preferred_username", user["displayName"]!!)
            }
        }
    }
}

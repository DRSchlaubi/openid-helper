package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

fun ProviderRegistry.bitly() = registerProvider("bitly") {
    authorize("https://bitly.com/oauth/authorize")
    token {
        request {
            url("https://api-ssl.bitly.com/oauth/access_token")
        }
        response {
            json {
                put("token_type", "bearer")
            }
        }
    }

    userEndpoint {
        request {
            fixLowercaseBearer()
            url("https://api-ssl.bitly.com/v4/user")
        }

        response {
            json { (data) ->
                put("sub", data["login"]!!)
                put("preferred_username", data["login"]!!)

                val email = data["emails"]?.jsonArray?.firstOrNull()?.jsonObject?.get("email")
                if (email != null) put("email", email)
            }
        }
    }
}

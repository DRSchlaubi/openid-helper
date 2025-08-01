package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.tumblr() = registerProvider("tumlbr") {
    authorize("https://www.tumblr.com/oauth2/authorize")
    token("https://api.tumblr.com/v2/oauth2/token")

    userEndpoint {
        request {
            url("https://api.tumblr.com/v2/user/info")
            fixLowercaseBearer()
        }

        response {
            json {(data) ->
                val response = data["response"]!!.jsonObject
                val user = response["user"]!!.jsonObject
                put("sub", user["name"]!!)
                put("preferred_username", user["name"]!!)
            }
        }
    }
}

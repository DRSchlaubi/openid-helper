package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.gumroad() = registerProvider("gumroad") {
    authorize("https://gumroad.com/oauth/authorize")
    token("https://api.gumroad.com/oauth/token")

    userEndpoint {
        request { url("https://api.gumroad.com/v2/user") }
        response {
            json { (data) ->
                val user = data["user"]!!.jsonObject

                put("sub", user["user_id"]!!)
                put("email", user["email"]!!)
                user["twitter_handle"]?.let { put("preferred_username", it) }
            }
        }
    }
}

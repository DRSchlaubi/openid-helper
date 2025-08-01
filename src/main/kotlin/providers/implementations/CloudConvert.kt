package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.cloudConvert() =registerProvider("cloudconvert") {
    authorize("https://cloudconvert.com/oauth/authorize")
    token("https://cloudconvert.com/oauth/token")

    userEndpoint {
        request { url("https://api.cloudconvert.com/v2/users/me") }
        response {
            json { (json) ->
                val data = json["data"]!!.jsonObject

                put("sub", data["id"]!!)
                put("preferred_username", data["email"]!!)
                put("email", data["email"]!!)
            }
        }
    }
}
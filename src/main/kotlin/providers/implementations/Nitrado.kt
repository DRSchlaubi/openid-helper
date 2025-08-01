package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import kotlinx.serialization.json.jsonObject


fun ProviderRegistry.nitrado() = registerProvider("nitrado") {
    authorize("https://oauth.nitrado.net/oauth/v2/auth")
    token("https://oauth.nitrado.net/oauth/v2/token")

    userEndpoint {
        request { url("https://api.nitrado.net/user\n") }
        response {
            json { (json) ->
                val data = json["data"]!!.jsonObject
                val user = data["user"]!!.jsonObject

                put("sub", user["user_id"]!!)
                put("preferred_username", user["username"]!!)
                put("email", user["email"]!!)
            }
        }
    }
}

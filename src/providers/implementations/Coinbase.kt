package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.coinbase() = registerProvider("coinbase") {
    authorize("https://login.coinbase.com/oauth2/auth")
    token("https://login.coinbase.com/oauth2/token")

    userEndpoint {
        request {
            url("https://api.coinbase.com/v2/user ")
        }

        response {
            json { (json) ->
                val data = json["data"]!!.jsonObject
                put("sub", data["id"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}

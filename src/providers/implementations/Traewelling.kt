package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.traewelling() = registerProvider("traewelling") {
    authorize("https://traewelling.de/oauth/authorize")
    token("https://traewelling.de/oauth/token")

    userEndpoint {
        request {
            url("https://traewelling.de/api/v1/auth/user")
        }

        response {
            json { (json) ->
                val data = json["data"]!!.jsonObject
                put("sub", json["id"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}
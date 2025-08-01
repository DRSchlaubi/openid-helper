package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

fun ProviderRegistry.digitalOcean() = registerProvider("digital-ocean") {
    authorize("https://cloud.digitalocean.com/v1/oauth/authorize")
    token {
        request {
            url("https://cloud.digitalocean.com/v1/oauth/token")
        }

        response {
            json {
                put("token_type", "bearer")
            }
        }
    }

    userEndpoint {
        request {
            url("https://api.digitalocean.com/v2/account")
        }

        response {
            json {(data) ->
                val account = data["account"]!!.jsonObject
                put("sub", account["uuid"]!!)
                put("email", account["email"]!!)
            }
        }
    }
}

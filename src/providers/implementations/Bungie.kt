package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.header
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.bungie() = registerProvider("bungie") {
    authorize("https://www.bungie.net/en/oauth/authorize")
    token("https://www.bungie.net/platform/app/oauth/token/")

    userEndpoint {
        request {
            url("https://www.bungie.net/Platform/User/GetMembershipsForCurrentUser/")

            json { (_, _, request) ->
                request.header("X-Api-Key", Config.BUNGE_API_KEY)
            }
        }

        response {
            json { (data) ->
                val response = data["Response"]!!.jsonObject
                val user = response["bungieNetUser"]!!.jsonObject

                put("sub", user["membershipId"]!!)
                put("preferred_username", user["uniqueName"]!!)
            }
        }
    }
}

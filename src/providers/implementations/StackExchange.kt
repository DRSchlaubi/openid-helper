package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.*

fun ProviderRegistry.stackExchange() = registerProvider("stackexchange") {
    authorize("https://stackexchange.com/oauth")
    token {
        request {
            url("https://stackexchange.com/oauth/access_token")
        }

        response {
            plainText<JsonObject> { (data) ->
                val body = data.toByteArray().decodeToString().parseUrlEncodedParameters()
                buildJsonObject {
                    put("access_token", body["access_token"])
                    put("expires_in", body["expires"])
                    put("token_type", "bearer")
                }
            }
        }
    }

    userEndpoint {
        request {
            url("https://api.stackexchange.com/2.3/me?site=stackoverflow")
        }

        response {
            json { (data) ->
                val user = data["items"]!!.jsonArray.first().jsonObject
                put("sub", user["account_id"]!!)
            }
        }
    }
}

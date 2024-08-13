package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.basicAuth
import kotlinx.serialization.json.jsonObject

fun ProviderRegistry.fitbit() = registerProvider("fitbit") {
    authorize("https://www.fitbit.com/oauth2/authorize")
    token {
        request {
            formBody({ form, _ -> basicAuth(form["client_id"]!!, form["client_secret"]!!) }, {
                remove("client_id")
                remove("client_secret")
            })
            url("https://api.fitbit.com/oauth2/token")
        }
    }

    userEndpoint {
        request {
            url("https://api.fitbit.com/1/user/-/profile.json")
        }

        response {
            json {
                val sub = it["user"]!!.jsonObject["encodedId"]!!
                put("sub", sub)
            }
        }
    }
}

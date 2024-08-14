package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.useHeaderForOAuthClientCredentials
import io.ktor.client.request.bearerAuth
import io.ktor.server.request.authorization

fun ProviderRegistry.eBay() = registerProvider("ebay") {
    authorize("https://auth.ebay.com/oauth2/authorize")
    token {
        request {
            useHeaderForOAuthClientCredentials()
            url("https://api.ebay.com/identity/v1/oauth2/token")
        }
    }

    userEndpoint {
        request {
            json({ _, call ->
                bearerAuth(call.request.authorization()?.drop("User Access Token ".length).toString())
            }) {}
            url("https://apiz.ebay.com/commerce/identity/v1/user/")
        }

        response {
            json {
                put("sub", it["userId"]!!)
            }
        }
    }
}

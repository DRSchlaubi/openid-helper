package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.eBay() = registerProvider("ebay") {
    authorize("https://auth.ebay.com/oauth2/authorize")
    token("https://api.ebay.com/identity/v1/oauth2/token")

    userEndpoint {
        request {
            url("https://apiz.ebay.com/commerce/identity/v1/user/")
        }

        response {
            json {
                put("sub", it["user_id"]!!)
            }
        }
    }
}

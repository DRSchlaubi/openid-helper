package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.amazon() = registerProvider("amazon") {
    authorize("https://www.amazon.com/ap/oa")
    token(Config.AMAZON_REGION.tokenUrl)
    userEndpoint {
        request {
            url("https://api.amazon.com/user/profile")
        }

        response {
            json {
                put("sub", it["user_id"]!!)
                put("email", it["email"]!!)
            }
        }
    }
}

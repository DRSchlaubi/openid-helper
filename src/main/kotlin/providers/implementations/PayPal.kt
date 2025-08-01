package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.useHeaderForOAuthClientCredentials

fun ProviderRegistry.paypal() = registerProvider("paypal") {
    jwksEndpoint = "https://api.paypal.com/v1/oauth2/certs"
    authorize("https://www.paypal.com/connect")
    token {
        request {
            useHeaderForOAuthClientCredentials()
            url("https://api-m.paypal.com/v1/oauth2/token")
        }
    }

    userEndpoint {
        request {
            url("https://api.paypal.com/v1/identity/openidconnect/userinfo?schema=openid")
        }
    }
}

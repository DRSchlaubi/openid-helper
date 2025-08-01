package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry

fun ProviderRegistry.klarna() = oauth2PKCE(
    "klarna",
    "https://login.klarna.com/eu/lp/idp/oauth2/auth",
    "https://login.klarna.com/eu/lp/idp/oauth2/token"
) {
    jwksEndpoint = "https://login.klarna.com/eu/lp/idp/.well-known/jwks.json"
    userEndpoint("https://login.klarna.com/eu/lp/idp/userinfo")
}

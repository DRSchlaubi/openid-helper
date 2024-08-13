package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.basicAuth

fun ProviderRegistry.epicGames() = registerProvider("epic-games") {
    jwksEndpoint = "https://api.epicgames.dev/epic/oauth/v2/.well-known/jwks.json"
    authorize("https://www.epicgames.com/id/authorize")
    userEndpoint("https://api.epicgames.dev/epic/oauth/v2/userInfo")

    token {
        request {
            url("https://api.epicgames.dev/epic/oauth/v2/token")

            formBody({form, _->
                basicAuth(form["client_id"]!!, form["client_secret"]!!)
            }) {
                append("deployment_id", Config.EPIC_DEPLOYMENT_ID)
            }
        }
    }
}

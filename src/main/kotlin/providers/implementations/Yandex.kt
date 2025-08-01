package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider

fun ProviderRegistry.yandex() = registerProvider("yandex") {
    authorize("https://oauth.yandex.com/authorize")
    token("https://oauth.yandex.com/token")

    userEndpoint {
        request { url("https://login.yandex.ru/info") }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["login"]!!)
                put("email", data["default_email"]!!)
            }
        }
    }
}

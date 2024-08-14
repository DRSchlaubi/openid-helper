package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.fixLowercaseBearer
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import kotlinx.serialization.json.put

fun ProviderRegistry.figma() = registerProvider("figma") {
    authorize("https://www.figma.com/oauth")
    token {
        request {
            url("https://www.figma.com/api/oauth/token")
            formBody({ _, _ -> accept(ContentType.Application.Json) }) {}
        }
        response {
            json {
                it.forEach(::put)
                put("token_type", "bearer")
            }
        }

    }

    userEndpoint {
        request {
            url("https://api.figma.com/v1/me")
            fixLowercaseBearer()
        }

        response {
            json {
                put("sub", it["id"]!!)
            }
        }
    }
}
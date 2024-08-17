package dev.schlaubi.openid.helper.providers.implementations

import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*

fun ProviderRegistry.heroku() = registerProvider("heroku") {
    authorize("https://id.heroku.com/oauth/authorize")
    token("https://id.heroku.com/oauth/token")

    userEndpoint {
        request {
            url("https://api.heroku.com/account")

            plainText<OutgoingContent.NoContent> { (_, _, request) ->
                request.accept(ContentType("application", "vnd.heroku+json").withParameter("version", "3"))
                EmptyContent
            }
        }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("email", data["email"]!!)
            }
        }
    }
}

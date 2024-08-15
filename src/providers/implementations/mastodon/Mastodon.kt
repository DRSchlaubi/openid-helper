package dev.schlaubi.openid.helper.providers.implementations.mastodon

import dev.schlaubi.openid.helper.Mastodon
import dev.schlaubi.openid.helper.buildUrl
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.resources.href
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun ProviderRegistry.mastodon() = registerProvider("mastodon") {
    authorize {
        val clientId = it.parameters["client_id"]!!
        val scope = it.parameters["scope"]!!
        val redirectUri = it.parameters["redirect_uri"]!!
        val state = it.parameters["state"]!!
        val responseType = it.parameters["response_type"]!!

        it.application.href(
            Mastodon.SelectHost(
                clientId, scope, redirectUri, state, responseType
            ), this
        )
    }

    token {
        request {
            formBody { (parameters, _, response) ->
                response.url.takeFrom(verifyToken(parameters["code"]!!).url)
                response.url.path("oauth", "token")
                response.contentType(ContentType.Application.FormUrlEncoded)

                val token = verifyToken(get("code")!!)
                val instance = getClient(token.url) ?: throw BadRequestException("Invalid instance")

                set("code", token.token)
                set("client_id", instance.clientId)
                set("client_secret", instance.clientSecret)
                set("redirect_uri", instance.redirectUri)
            }
        }

        response {
            json { (data, response) ->
                val url = response.call.request.url.buildUrl {
                    encodedPath = ""
                }
                put("access_token", newMappedToken(url, data["access_token"]!!.jsonPrimitive.content))
            }
        }
    }

    userEndpoint {
        request {
            url {
                takeFrom(it.auth.url)
                path("api", "v1", "accounts", "verify_credentials")
            }

            formBody { (_, call, request) ->
                request.bearerAuth(call.auth.token)
            }
        }

        response {
            json { (data) ->
                put("sub", data["id"]!!)
                put("preferred_username", data["username"]!!)
            }
        }
    }
}

private val ApplicationCall.auth: Token
    get() = verifyToken((request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob.toString())


package dev.schlaubi.openid.helper.providers.implementations

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.kord.cache.api.put
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.httpClient
import dev.schlaubi.openid.helper.providers.ProviderRegistry
import dev.schlaubi.openid.helper.providers.registerProvider
import dev.schlaubi.openid.helper.util.State
import dev.schlaubi.openid.helper.util.cache
import dev.schlaubi.openid.helper.util.findAndRemoveState
import dev.schlaubi.openid.helper.util.registerState
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

@Serializable
data class OpenIdState(override val id: String, val assocHandle: String, val redirectUri: String) : State

fun ProviderRegistry.ubuntu() = openid("ubuntu", Url("https://login.ubuntu.com/+openid"))
fun ProviderRegistry.openSUSE() = openid("opensuse", Url("https://id.opensuse.org/openid"))
fun ProviderRegistry.fedora() = openid("fedora", Url("https://id.fedoraproject.org/openid"))

private fun newKey(user: JsonObject) = JWT.create()
    .withClaim("user", Json.encodeToString(user))
    .sign(Algorithm.HMAC256(Config.JWT_SECRET))

private val verifier = JWT.require(Algorithm.HMAC256(Config.JWT_SECRET)).build()

fun ProviderRegistry.openid(name: String, endpoint: Url) = registerProvider(name) {
    suspend fun RoutingContext.handleCallback(response: Parameters) {
        val state = findAndRemoveState<OpenIdState>(call.request.cookies["state"])
            ?: throw BadRequestException("Missing state")

        val verifyResponse = httpClient.post(endpoint) {
            val parameters = Parameters.build {
                appendAll(response)
                set("openid.mode", "check_authentication")
                set("openid.assoc_handle", state.assocHandle)
            }

            setBody(FormDataContent(parameters))
        }.parseOpenIdResponse()

        if (verifyResponse["is_valid"] != "true") {
            val user = buildJsonObject {
                fun putOpenid(key: String, responseKey: String) {
                    val value = response["openid.ax.value.$responseKey.1"] ?: return
                    put(key, value)
                }
                put("sub", (response["openid.ax.value.email.1"] ?: response["openid.ax.value.nickname.1"])!!)
                putOpenid("preferred_nickname", "nickname")
                putOpenid("email", "email")
                putOpenid("given_name", "first_name")
                putOpenid("family_name", "last_name")
                putOpenid("name", "fullname")
            }

            val code = newKey(user)

            call.respondRedirect {
                takeFrom(state.redirectUri)

                parameters.append("code", code)
                parameters.append("state", state.id)
            }
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }


    onStartup { registerState<OpenIdState>() }
    authorize {
        val redirectUri = it.parameters["redirect_uri"] ?: throw BadRequestException("Missing redirect_uri")
        val state = it.parameters["state"] ?: throw BadRequestException("Missing state")
        takeFrom(it.application.fullHref(ProviderRoute.InitiateOpenid(name)))

        val response = httpClient.post(endpoint) {
            val parameters = Parameters.build {
                append("openid.mode", "associate")
            }

            setBody(FormDataContent(parameters))
        }.parseOpenIdResponse()

        cache.put(OpenIdState(state, response["assoc_handle"]!!, redirectUri))
        it.response.cookies.append(
            "state",
            state,
            path = "/",
            expires = GMTDate() + 2.minutes
        )
    }

    routing {
        get<ProviderRoute.InitiateOpenid> {
            call.respondText(ContentType.Text.Html) {
                //language=HTML
                """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Redirecting...</title>
                    </head>
                    <body>
                        <form id="redirectForm" action="$endpoint" method="POST">
                            <input type="hidden" name="openid.ns" value="http://specs.openid.net/auth/2.0">
                            <input type="hidden" name="openid.ns.ax" value="http://openid.net/srv/ax/1.0">
                            <input type="hidden" name="openid.ax.mode" value="fetch_request">
                            <input type="hidden" name="openid.ax.type.email" value="http://axschema.org/contact/email">
                            <input type="hidden" name="openid.ax.type.fullname" value="http://axschema.org/namePerson">
                            <input type="hidden" name="openid.ax.type.first_name" value="http://axschema.org/namePerson/first">
                            <input type="hidden" name="openid.ax.type.last_name" value="http://axschema.org/namePerson/last">
                            <input type="hidden" name="openid.ax.type.nickname" value="http://axschema.org/namePerson/friendly">
                            <input type="hidden" name="openid.ax.type.old_email" value="http://schema.openid.net/contact/email">
                            <input type="hidden" name="openid.ax.type.old_fullname" value="http://schema.openid.net/namePerson">
                            <input type="hidden" name="openid.ax.type.old_nickname" value="http://schema.openid.net/namePerson/friendly">
                            <input type="hidden" name="openid.ax.required" value="email,fullname,first_name,last_name,nickname,old_email,old_fullname,old_nickname">
                            <input type="hidden" name="openid.mode" value="checkid_setup">
                            <input type="hidden" name="openid.realm" value="${Config.HOSTNAME}">
                            <input type="hidden" name="openid.trust_root" value="${Config.HOSTNAME}">
                            <input type="hidden" name="openid.return_to" value="${
                    call.application.fullHref(
                        ProviderRoute.Callback(name)
                    )
                }">
                            <input type="hidden" name="openid.identity" value="http://specs.openid.net/auth/2.0/identifier_select">
                            <input type="hidden" name="openid.claimed_id" value="http://specs.openid.net/auth/2.0/identifier_select">
                        </form>
                        <script type="text/javascript">
                            document.getElementById("redirectForm").submit();
                        </script>
                    </body>
                    </html>
                """.trimIndent()
            }
        }

        get<ProviderRoute.Callback> {
            handleCallback(call.parameters)
        }

        post<ProviderRoute.Callback> {
            val response = call.receiveParameters()

            handleCallback(response)
        }

        post<ProviderRoute.Token> {
            val response = call.receiveParameters()
            val code = response["code"] ?: throw BadRequestException("Missing code")
            if (response["grant_type"] != "authorization_code") throw BadRequestException("Invalid type")

            val tokenResponse = buildJsonObject {
                put("access_token", code)
                put("token_type", "bearer")
            }

            call.respond(tokenResponse)
        }

        get<ProviderRoute.UserInfo> {
            val token = (call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob
                ?: throw BadRequestException("Missing token")
            call.respondText(verifier.verify(token).getClaim("user").asString(), ContentType.Application.Json)
        }
    }
}

private suspend fun HttpResponse.parseOpenIdResponse(): Map<String, String> {
    return bodyAsText().lineSequence().filter(String::isNotBlank).associate { row ->
        val (key, value) = row.split(":")
        key to value
    }
}

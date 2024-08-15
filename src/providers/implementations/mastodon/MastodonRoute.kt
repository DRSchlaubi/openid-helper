package dev.schlaubi.openid.helper.providers.implementations.mastodon

import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.Mastodon
import dev.schlaubi.openid.helper.fullHref
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveParameters
import io.ktor.server.resources.get
import io.ktor.server.resources.href
import io.ktor.server.resources.post
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.input
import kotlinx.html.meta
import kotlinx.html.onChange
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import org.intellij.lang.annotations.Language
import kotlin.text.startsWith


private val LOG = KotlinLogging.logger { }

@Language("CSS")
private val css = """
@import url('https://fonts.googleapis.com/css2?family=Roboto&display=swap');
body {
    background-color: black;
    font-family: 'Roboto', sans-serif;
    text-align: center;
}

input {
    background-color: inherit; /* Uses the normal background color of its parent */
    border-radius: 8px; /* Adjust the radius as needed for desired rounded effect */
    padding: 10px; /* Optional: Adds some padding for better appearance */
    border: 1px solid #ccc; /* Optional: Adds a border color */
}
form input {
    display: block;
    margin-bottom: 10px; /* Adds space between inputs */
    width: 100%; /* Makes inputs take full width of the container */
    box-sizing: border-box; /* Ensures padding and border are included in the element's total width and height */
}

.centered-div {
    position: fixed;
    color: white;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background-color: rgb(43, 45, 49);
    border-radius: 15px;
    padding: 20px;
    text-align: center;
}
.error {
    color: red
}

@media screen and (max-width: 600px) {
    .centered-div {
        width: 100% !important;
        height: 100% !important;
        top: 0 !important;
        left: 0 !important;
        transform: none !important;
        border-radius: 0;
    }
}    
/* Style for the button to differentiate interactively */
.centered-div form button {
    background-color: darkgray;
    color: white; /* Text color for the button */
    cursor: pointer; /* Changes cursor to pointer on hover */
    border-radius: 8px; /* More rounded corners */
    padding: 8px;
    border: none; /* Removes the default border */
}

""".trimIndent()

private data class State(val app: MastodonOAuthApplication, val redirect: String, val url: String)

private val states = mutableMapOf<String, State>()

fun Route.mastodon() {
    get<Mastodon.SelectHost> { route ->
        call.verifyOauth()
        val (_, _, _, _, _, isInvalid, prefill) = route
        call.respondHtml {
            head {
                title = "Mastodon selector"
                meta("viewport", "width=device-width, initial-scale=1.0")
            }

            body {
                style {
                    +css
                }

                div("centered-div") {
                    img(
                        alt = "Mastodon logo",
                        src = "https://files.mastodon.social/accounts/avatars/000/013/179/original/b4ceb19c9c54ec7e.png"
                    ) {
                        height = "128"
                        width = "128"
                    }
                    h2 { +"Please enter the Mastodon host below" }
                    if (isInvalid) {
                        p("error") {
                            id = "error-message"
                            +"This host is invalid"
                        }
                    }
                    form(method = FormMethod.post) {
                        input(InputType.text, name = "host") {
                            required = true
                            placeholder = "mastodon.social"
                            //language=JavaScript
                            onChange = """
                                document.getElementById("submit-button").disabled = false
                                document.getElementById("error-message").remove()
                            """.trimIndent()
                            if (prefill != null) {
                                value = prefill
                            }
                        }

                        button(type = ButtonType.submit) {
                            id = "submit-button"
                            disabled = isInvalid
                            +"Submit"
                        }
                    }
                }
            }
        }
    }

    post<Mastodon.SelectHost> {
        call.verifyOauth()
        val mastodonHost = call.receiveParameters()["host"] ?: throw BadRequestException("Missing host")
        val url = URLBuilder().apply {
            if (!mastodonHost.startsWith("http")) {
                protocol = URLProtocol.HTTPS
                host = mastodonHost
            } else {
                takeFrom(mastodonHost)
            }
        }.build()
        val client = getClient(url.toString()) ?: run {
            val applicationResponse = try {
                registerMastodonApplication(
                    it.scope,
                    call.application.fullHref(Mastodon.CallbackBase()),
                    url,
                    Config.MASTODON_NAME
                )
            } catch (e: Exception) {
                LOG.debug(e) { "Could not register application" }
                null
            }
            if (applicationResponse != null && applicationResponse.status.isSuccess()) {
                applicationResponse.body<MastodonOAuthApplication>().also {
                    addClient(url.toString(), it)
                }
            } else {
                val body = applicationResponse?.bodyAsText()
                LOG.error { "Could not register application: $body" }
                call.respondRedirect(call.application.href(it.copy(prefill = url.toString(), isInvalid = true)))
                return@post
            }
        }

        states[it.state] = State(client, it.redirectUri, url.toString())

        val redirectUri = call.application.fullHref(Mastodon.CallbackBase())

        call.respondRedirect {
            takeFrom(url)
            path("oauth", "authorize")
            parameters.append("client_id", client.clientId)
            parameters.append("response_type", it.responseType)
            parameters.append("scope", it.scope)
            parameters.append("state", it.state)
            parameters.append("redirect_uri", redirectUri)
        }
    }

    get<Mastodon.Callback> { (code, state) ->
        val foundState = states.remove(state) ?: throw BadRequestException("Missing state")
        val authCode = newMappedToken(foundState.url, code)
        call.respondRedirect {
            takeFrom(foundState.redirect)
            parameters["code"] = authCode
            parameters.append("state", state)
        }
    }
}

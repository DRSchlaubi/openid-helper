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
import io.ktor.server.http.content.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveParameters
import io.ktor.server.resources.get
import io.ktor.server.resources.href
import io.ktor.server.resources.post
import io.ktor.server.response.*
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
import kotlinx.html.unsafe
import org.intellij.lang.annotations.Language
import java.io.File
import java.net.URI
import kotlin.text.startsWith


private val LOG = KotlinLogging.logger { }

private data class State(val app: MastodonOAuthApplication, val redirect: String, val url: String)

private val states = mutableMapOf<String, State>()

fun Route.mastodon() {
    get<Mastodon.SelectHost> { route ->
        call.verifyOauth()
        val (_, _, _, _, _, isInvalid, prefill) = route

        val file = ClassLoader.getSystemResource("mastodon/select-host.html")
        call.respondFile(File(file.toURI()))
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

package dev.schlaubi.openid.helper.providers.implementations.mastodon

import dev.kord.cache.api.put
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.Mastodon
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.util.State
import dev.schlaubi.openid.helper.util.cache
import dev.schlaubi.openid.helper.util.findAndRemoveState
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.Serializable


private val LOG = KotlinLogging.logger { }

@Serializable
data class MastodonState(
    override val id: String,
    val app: MastodonOAuthApplication,
    val redirect: String,
    val url: String
) : State

fun Route.mastodon() {
    get<Mastodon.SelectHost> {
        call.verifyOauth()

        val file = ClassLoader.getSystemResourceAsStream("mastodon/select-host.html")
        call.respond(object : OutgoingContent.ReadChannelContent() {
            override fun readFrom(): ByteReadChannel = file!!.toByteReadChannel()
            override val contentType: ContentType = ContentType.Text.Html
        })
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

        cache.put(MastodonState(it.state, client, it.redirectUri, url.toString()))

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
        val foundState = findAndRemoveState<MastodonState>(state) ?: throw BadRequestException("Missing state")
        val authCode = newMappedToken(foundState.url, code)
        call.respondRedirect {
            takeFrom(foundState.redirect)
            parameters["code"] = authCode
            parameters.append("state", state)
        }
    }
}

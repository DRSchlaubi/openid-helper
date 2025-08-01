package dev.schlaubi.openid.helper.providers.implementations.bluesky

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWKSet
import dev.kord.cache.api.put
import dev.schlaubi.openid.helper.Config
import dev.schlaubi.openid.helper.ProviderRoute
import dev.schlaubi.openid.helper.fullHref
import dev.schlaubi.openid.helper.util.cache
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.resources.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.SerialName

const val dpopKeyId = "key1"

val didPattern = "^did:(plc|web):([a-zA-Z0-9_\\-.]+)$".toRegex()

private fun blueSkyRoute() = ProviderRoute("bluesky")

class BlueSkyRoute {
    @Resource("metadata")
    data class ClientMetadata(val parent: ProviderRoute = blueSkyRoute())

    @Resource("keys")
    data class Keys(val parent: ProviderRoute = blueSkyRoute())

    @Resource("callback")
    class CallbackRoute(val parent: ProviderRoute = blueSkyRoute())

    @Resource("")
    data class Callback(
        val state: String,
        val code: String,
        val iss: String,
        val parent: CallbackRoute = CallbackRoute()
    )

    @Resource("initiate-login")
    data class InitiateLogin(
        val state: String,
        val scope: String,
        @SerialName("client_id")
        val clientId: String,
        @SerialName("redirect_uri")
        val redirectUri: String,
        @SerialName("is-invalid")
        val isInvalid: Boolean = false,
        val prefill: String? = null,
        val parent: ProviderRoute = blueSkyRoute()
    )
}

fun Route.blueskyRoute() {
    get<BlueSkyRoute.ClientMetadata> {
        call.respond(
            ClientMetadata(
                fullHref(BlueSkyRoute.ClientMetadata()),
                "web",
                listOf("authorization_code"),
                listOf("code"),
                listOf(fullHref(BlueSkyRoute.CallbackRoute())),
                "atproto transition:generic transition:chat.bsky",
                true,
                "private_key_jwt",
                "ES256",
                Config.BLUESKY_CLIENT_NAME,
                Config.BLUESKY_CLIENT_URI,
                Config.BLUESKY_LOGO_URI,
                Config.BLUESKY_TOS_URI,
                Config.BLUESKY_PRIVACY_POLICY_URI,
                fullHref(BlueSkyRoute.Keys())
            )
        )
    }

    get<BlueSkyRoute.Keys> {
        val publicKey = Config.PUBLIC_DPOP_SIGNING_KEY
        val curve = Curve.forECParameterSpec(publicKey.params)
        val key = ECKey.Builder(curve, publicKey)
            .keyID(dpopKeyId)
            .build()

        call.respondText(ContentType.Application.Json) { JWKSet(key).toString() }
    }

    get<BlueSkyRoute.InitiateLogin> {
        val file = ClassLoader.getSystemResourceAsStream("bluesky/select-handle.html")
        call.respond(object : OutgoingContent.ReadChannelContent() {
            override fun readFrom(): ByteReadChannel = file!!.toByteReadChannel()
            override val contentType: ContentType = ContentType.Text.Html
        })
    }

    post<BlueSkyRoute.InitiateLogin> { (state, scope, clientId, redirectUri) ->
        call.verifyRequest()

        val handle = call.receiveParameters()["handle"] ?: throw BadRequestException("Missing handle")
        val loginInfo = try {
            if (handle.matches(didPattern)) {
                resolveDid(handle)
            } else {
                resolveDomainName(handle)
            }
        } catch (e: Exception) {
            return@post call.respondRedirect(
                call.application.href(
                    BlueSkyRoute.InitiateLogin(
                        state,
                        scope,
                        clientId,
                        redirectUri,
                        true,
                        handle
                    )
                )
            )
        }

        val par = requestPAR(loginInfo, scope, state, redirectUri)
        cache.put(par)
        call.respondRedirect {
            takeFrom(loginInfo.authServer.server.authorizationEndpoint)
            parameters["request_uri"] = par.requestUri
            parameters["client_id"] = blueSkyClientId
            parameters["state"] = state
        }
    }
}

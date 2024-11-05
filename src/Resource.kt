package dev.schlaubi.openid.helper

import dev.schlaubi.openid.helper.providers.Provider
import dev.schlaubi.openid.helper.providers.providers
import io.ktor.resources.*
import io.ktor.server.plugins.*
import kotlinx.serialization.SerialName

interface HasProvider {
    val parent: ProviderRoute

    val provider: Provider
        get() = parent.provider
}

@Resource("/providers/{providerName}")
data class ProviderRoute(val providerName: String) {
    val provider: Provider
        get() = providers[providerName] ?: throw NotFoundException("Provider $providerName not found")

    @Resource("openid-configuration")
    data class OpenIDConfiguration(override val parent: ProviderRoute) : HasProvider

    @Resource("authorize")
    data class Authorize(override val parent: ProviderRoute) : HasProvider {
        constructor(name: String) : this(ProviderRoute(name))
    }

    @Resource("token")
    data class Token(override val parent: ProviderRoute) : HasProvider {
        constructor(name: String) : this(ProviderRoute(name))
    }

    @Resource("user")
    data class UserInfo(override val parent: ProviderRoute) : HasProvider {
        constructor(name: String) : this(ProviderRoute(name))
    }

    @Resource("callback")
    data class Callback(override val parent: ProviderRoute) : HasProvider {
        constructor(name: String) : this(ProviderRoute(name))
    }


    @Resource("initiate-openid")
    data class InitiateOpenid(override val parent: ProviderRoute) : HasProvider {
        constructor(name: String) : this(ProviderRoute(name))
    }
}

@Resource("/mastodon")
class Mastodon {
    @Resource("select-host")
    data class SelectHost(
        @SerialName("client_id") val clientId: String,
        @SerialName("scope") val scope: String,
        @SerialName("redirect_uri") val redirectUri: String,
        @SerialName("state") val state: String,
        @SerialName("response_type") val responseType: String,
        @SerialName("is-invalid") val isInvalid: Boolean = false,
        val prefill: String? = null,
        val parent: ProviderRoute = ProviderRoute("mastodon")
    )

    @Resource("callback")
    data class Callback(val code: String, val state: String, val parent: ProviderRoute = ProviderRoute("mastodon"))

    @Resource("callback")
    data class CallbackBase(val parent: ProviderRoute = ProviderRoute("mastodon"))
}

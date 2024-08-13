package dev.schlaubi.openid.helper

import dev.schlaubi.openid.helper.providers.Provider
import dev.schlaubi.openid.helper.providers.providers
import io.ktor.resources.Resource
import io.ktor.server.plugins.NotFoundException

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
}

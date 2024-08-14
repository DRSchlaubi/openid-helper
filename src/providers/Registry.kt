package dev.schlaubi.openid.helper.providers

import dev.schlaubi.openid.helper.providers.implementations.amazon
import dev.schlaubi.openid.helper.providers.implementations.digitalOcean
import dev.schlaubi.openid.helper.providers.implementations.eBay
import dev.schlaubi.openid.helper.providers.implementations.epicGames
import dev.schlaubi.openid.helper.providers.implementations.fitbit
import dev.schlaubi.openid.helper.providers.implementations.imgur
import dev.schlaubi.openid.helper.providers.implementations.tumblr
import dev.schlaubi.openid.helper.providers.implementations.wordpressCom
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias ProviderRegistry = MutableMap<String, Provider>

val providers = buildMap<String, Provider> {
    epicGames()
    amazon()
    eBay()
    fitbit()
    wordpressCom()
    tumblr()
    digitalOcean()
    imgur()
}

@OptIn(ExperimentalContracts::class)
fun ProviderRegistry.registerProvider(name: String, builder: ProviderBuilder.() -> Unit) {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    this[name] = provider(name, builder)
}

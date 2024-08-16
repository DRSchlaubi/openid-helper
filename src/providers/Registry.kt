package dev.schlaubi.openid.helper.providers

import dev.schlaubi.openid.helper.providers.implementations.*
import dev.schlaubi.openid.helper.providers.implementations.mastodon.mastodon
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
    strava()
    figma()
    atlassian()
    traewelling()
    mastodon()
    bitbucket()
    coinbase()
    box()
    bungie()
    dribbble()
    vk()
    yandex()
    osu()
    rocketbeans()
    vimeo()
    linode()
}

@OptIn(ExperimentalContracts::class)
fun ProviderRegistry.registerProvider(name: String, builder: ProviderBuilder.() -> Unit) {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    this[name] = provider(name, builder)
}

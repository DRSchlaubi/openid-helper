package dev.schlaubi.openid.helper.providers

import dev.schlaubi.openid.helper.providers.implementations.*
import dev.schlaubi.openid.helper.providers.implementations.mastodon.mastodon
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val LOG = KotlinLogging.logger { }

typealias ProviderRegistry = MutableMap<String, Provider>

internal var providers: Map<String, Provider> = mutableMapOf()
    private set

suspend fun providers() = buildMap {
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
    stackExchange()
    nitrado()
    mixcloud()
    dailymotion()
    deviantart()
    heroku()
    bitly()
    bbn()
    lastfm()
    paypal()
    cloudConvert()
    eveonline()
    klarna()
    gumroad()
    guilded()
    registerProviderCatching("Flickr", ::flickr)
    registerProviderCatching("Discogs", ::discogs)
}.onEach { (_, provider) ->
    coroutineScope {
        provider.register?.invoke(this)
    }
}
    .also { providers = it }

@OptIn(ExperimentalContracts::class)
inline fun ProviderRegistry.registerProvider(name: String, builder: ProviderBuilder.() -> Unit) {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    this[name] = provider(name, builder)
}

private inline fun registerProviderCatching(name: String, register: () -> Unit) {
    try {
        register()
    } catch (e: IllegalStateException) {
        LOG.warn(e) { "Could not register provider $name" }
    }
}

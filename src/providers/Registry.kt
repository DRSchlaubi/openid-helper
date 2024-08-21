package dev.schlaubi.openid.helper.providers

import dev.schlaubi.openid.helper.providers.implementations.*
import dev.schlaubi.openid.helper.providers.implementations.mastodon.mastodon
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val LOG = KotlinLogging.logger { }

typealias ProviderRegistry = MutableMap<String, Provider>

val providers = buildMap {
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
    try {
        flickr()
    } catch (e: IllegalStateException) {
        LOG.warn(e) { "Could not register Flickr provider" }
    }
}

@OptIn(ExperimentalContracts::class)
fun ProviderRegistry.registerProvider(name: String, builder: ProviderBuilder.() -> Unit) {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    this[name] = provider(name, builder)
}

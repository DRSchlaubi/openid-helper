package dev.schlaubi.openid.helper

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import dev.schlaubi.envconf.Config as EnvironmentConfig

enum class AmazonRegion(val tokenUrl: String) {
    NORTH_AMERICA("https://api.amazon.com/auth/o2/token"),
    EUROPE("https://api.amazon.co.uk/auth/o2/token"),
    FAR_EAST("https://api.amazon.co.jp/auth/o2/token"),
}

object Config : EnvironmentConfig() {
    val PORT by getEnv(8080, String::toInt)
    val HOST by getEnv("0.0.0.0")
    val HOSTNAME by getEnv(Url("http://localhost:8080"), ::Url)
    val EPIC_DEPLOYMENT_ID by this
    val AMAZON_REGION by getEnv(AmazonRegion.EUROPE, ::enumValueOf)
}

inline fun <reified R : Any> Application.fullHref(resource: R): String {
    return URLBuilder(Config.HOSTNAME).apply {
        href(resource, this)
    }.buildString()
}

@OptIn(ExperimentalContracts::class)
inline fun Url.buildUrl(urlBuilder: URLBuilder.() -> Unit): String {
    contract {
        callsInPlace(urlBuilder, InvocationKind.EXACTLY_ONCE)
    }
    return URLBuilder(this).apply(urlBuilder).buildString()
}

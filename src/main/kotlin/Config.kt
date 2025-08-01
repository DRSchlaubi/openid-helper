package dev.schlaubi.openid.helper

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.resources.serialization.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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
    val JWT_SECRET by getEnv("verrysecurenonsense")
    val MASTODON_NAME by getEnv("OpenID Helper")
    val MASTODON_CLIENT_ID by getEnv("appleistdoof")
    val MASTODON_CLIENT_SECRET by getEnv("werdasliestkannlesen")
    val MASTODON_REDIRECT_URI by this
    val BUNGE_API_KEY by this
    val FLICKR_CONSUMER_KEY by this
    val FLICKR_CONSUMER_SECRET by this
    val DISCOGS_CONSUMER_KEY by this
    val DISCOGS_CONSUMER_SECRET by this
    val REDIS_URL by getEnv().optional()

    @OptIn(ExperimentalEncodingApi::class)
    private val DPOP_SIGNING_KEY by getEnv {
        val pem = PEMParser(InputStreamReader(ByteArrayInputStream(Base64.decode(it))))
            .use(PEMParser::readObject) as PEMKeyPair

        JcaPEMKeyConverter().getKeyPair(pem)
    }

    val PUBLIC_DPOP_SIGNING_KEY get() = DPOP_SIGNING_KEY.public as ECPublicKey
    val PRIVATE_DPOP_SIGNING_KEY get() = DPOP_SIGNING_KEY.private as ECPrivateKey

    val BLUESKY_CLIENT_ID by this
    val BLUESKY_CLIENT_SECRET by this
    val BLUESKY_REDIRECT_URIS by getEnv { it.split(",\\s*".toRegex()) }

    val BLUESKY_CLIENT_NAME by getEnv().optional()
    val BLUESKY_CLIENT_URI by getEnv().optional()
    val BLUESKY_LOGO_URI by getEnv().optional()
    val BLUESKY_TOS_URI by getEnv().optional()
    val BLUESKY_PRIVACY_POLICY_URI by getEnv().optional()
}

@OptIn(ExperimentalContracts::class)
inline fun <reified R : Any> Application.fullHref(resource: R, build: URLBuilder.() -> Unit = {}): String {
    contract {
        callsInPlace(build, InvocationKind.AT_MOST_ONCE)
    }

    return URLBuilder(Config.HOSTNAME).apply {
        href(resource, this)
        build()
    }.buildString()
}

inline fun <reified R : Any> fullHref(resource: R): String {
    return URLBuilder(Config.HOSTNAME).apply {
        href(ResourcesFormat(), resource, this)
    }.buildString()
}

@OptIn(ExperimentalContracts::class)
inline fun Url.buildUrl(urlBuilder: URLBuilder.() -> Unit): String {
    contract {
        callsInPlace(urlBuilder, InvocationKind.EXACTLY_ONCE)
    }
    return URLBuilder(this).apply(urlBuilder).buildString()
}

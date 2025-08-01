package dev.schlaubi.openid.helper.providers.implementations.bluesky

import dev.schlaubi.openid.helper.dnsClient
import dev.schlaubi.openid.helper.httpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class InvalidHandleException(message: String? = null) : RuntimeException(message ?: "Could not validate handle")

data class LoginInformation(
    val authServer: AuthorizationServerInfo,
    val did: String? = null,
    val loginHint: String? = null
)

// https://docs.bsky.app/docs/advanced-guides/oauth-client#hardened-http-client-ssrf
fun HttpRequestBuilder.handleUnsafeRequest() {
    timeout {
        requestTimeoutMillis = 5000
        socketTimeoutMillis = 1000
        connectTimeoutMillis = 1000
    }
}

// https://atproto.com/specs/handle#handle-resolution
suspend fun resolveDomainName(domainName: String) = coroutineScope {
    val safeUrl = URLBuilder().apply {
        if (!domainName.startsWith("http")) {
            protocol = URLProtocol.HTTPS
            host = domainName
        } else {
            takeFrom(domainName)
        }
    }.build()

    val httpRequest = async {
        val response = httpClient.get(safeUrl) {
            url {
                path(".well-known", "atproto-did")
            }
            handleUnsafeRequest()
        }
        if (response.status.isSuccess()) {
            response.bodyAsText() // DID from HTTP
        } else {
            null
        }
    }

    val dnsLookup = async {
        val record = "_atproto.${safeUrl.host}"
        runCatching {
            val value = dnsClient.lookUp(record, "TXT")
            value.data.firstOrNull()?.removeSurrounding("\"")
                ?.parseUrlEncodedParameters()?.get("did")
        }.getOrNull()
    }

    val did = runCatching { dnsLookup.await() }
        .getOrNull() ?: runCatching { httpRequest.await() }.getOrNull()

    try {
        did?.let { resolveDid(it, domainName) }
            ?: LoginInformation(resolveOAuthServer(safeUrl.toString()))
    } catch (e: Exception) {
        throw InvalidHandleException(e.message)
    }
}

suspend fun resolveDid(did: String, hint: String? = null): LoginInformation = try {
    val didData = findPDSForDid(did)
    val pdsServer = didData.service.firstOrNull { it.type == "AtprotoPersonalDataServer" }
        ?.serviceEndpoint ?: throw InvalidHandleException()

    val authServer = resolveOAuthServer(pdsServer)

    LoginInformation(authServer, did, hint)
} catch (e: ResponseException) {
    throw InvalidHandleException()
}

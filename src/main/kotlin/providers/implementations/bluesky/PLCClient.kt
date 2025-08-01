package dev.schlaubi.openid.helper.providers.implementations.bluesky

import dev.schlaubi.openid.helper.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val plcUrl = "https://plc.directory/"

@Serializable
data class DIDDocument(
    @SerialName("@context")
    val context: List<String>,
    val id: String,
    val alsoKnownAs: List<String>,
    val service: List<Service>
) {
    @Serializable
    data class Service(
        val id: String,
        val type: String,
        val serviceEndpoint: String
    )
}

suspend fun findPDSForDid(did: String): DIDDocument {
    val (method, identifier) = didPattern.matchEntire(did)?.destructured ?: throw InvalidHandleException()

    // https://atproto.com/specs/did#blessed-did-methods
    return when (method) {
        // https://web.plc.directory/api/redoc#operation/ResolveDid
        "plc" -> resolveDidPLC(did)
        // https://w3c-ccg.github.io/did-method-web/
        "web" -> resolveDidWeb(identifier)

        else -> throw InvalidHandleException("Unsupported DID method: $method")
    }
}

private suspend fun resolveDidPLC(did: String): DIDDocument = httpClient.get(plcUrl) {
    url {
        appendPathSegments(did)
    }
}.apply { if (!status.isSuccess()) throw InvalidHandleException() }.body()

private suspend fun resolveDidWeb(identifier: String) = httpClient.get {
    url {
        protocol = URLProtocol.HTTPS
        host = identifier

        path(".well-known", "did.json")
    }
}.body<DIDDocument>()

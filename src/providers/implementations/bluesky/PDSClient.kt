package dev.schlaubi.openid.helper.providers.implementations.bluesky

import dev.schlaubi.openid.helper.httpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthProtectedResource(
    val resource: String,
    @SerialName("authorization_servers")
    val authorizationServers: List<String>,
    @SerialName("scopes_supported")
    val scopesSupported: List<String>,
    @SerialName("bearer_methods_supported")
    val bearerMethodsSupported: List<String>,
    @SerialName("resource_documentation")
    val resourceDocumentation: String
)

@Serializable
data class OAuthAuthorizationServer(
    val issuer: String,
    @SerialName("pushed_authorization_request_endpoint")
    val pushedAuthorizationRequestEndpoint: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("scopes_supported")
    val scopesSupported: List<String>
)

@Serializable
data class AuthorizationServerInfo(
    val resource: String,
    val server: OAuthAuthorizationServer,
)

suspend fun resolveOAuthServer(pdsServer: String): AuthorizationServerInfo {
    val resource = resolveProtectedOAuthResource(pdsServer)
    val authServer = resource.authorizationServers.first()
    return AuthorizationServerInfo(resource.resource, resolveAuthorizationServer(authServer))
}

private suspend fun resolveProtectedOAuthResource(domain: String) = httpClient.get(domain) {
    url {
        appendPathSegments(".well-known", "oauth-protected-resource")
    }
    handleUnsafeRequest()
    expectSuccess = true
}.body<OAuthProtectedResource>()

private suspend fun resolveAuthorizationServer(domain: String) = httpClient.get(domain) {
    url {
        appendPathSegments(".well-known", "oauth-authorization-server")
    }
    handleUnsafeRequest()
    expectSuccess = true
}.body<OAuthAuthorizationServer>()
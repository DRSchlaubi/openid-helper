package dev.schlaubi.openid.helper.providers.implementations.mastodon

import dev.schlaubi.openid.helper.buildUrl
import dev.schlaubi.openid.helper.httpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MastodonOAuthApplication(
    val id: String,
    val name: String,
    val website: String? = null,
    @SerialName("redirect_uris")
    val redirectUris: List<String> = emptyList(),
    @SerialName("redirect_uri")
    val redirectUri: String,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("vapid_key")
    val vapidKey: String,
    val scopes: List<String> = emptyList()
)

suspend fun registerMastodonApplication(scopes: String, redirectUri: String, url: Url, name: String): HttpResponse {
    val body = Parameters.build {
        append("client_name", name)
        append("redirect_uris", redirectUri)
        append("scopes", scopes)
    }
    return httpClient.submitForm(url.buildUrl { path("api/v1/apps") }, formParameters = body)
}

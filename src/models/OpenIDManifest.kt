package dev.schlaubi.openid.helper.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenIDManifest(
    val issuer: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("userinfo_endpoint")
    val userInfoEndpoint: String,
    @SerialName("jwks_uri")
    val jwksUri: String? = null
)

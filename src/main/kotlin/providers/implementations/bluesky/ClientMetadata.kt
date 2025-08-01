package dev.schlaubi.openid.helper.providers.implementations.bluesky

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientMetadata(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("application_type")
    val applicationType: String,
    @SerialName("grant_types")
    val grantTypes: List<String>,
    @SerialName("response_types")
    val responseTypes: List<String>,
    @SerialName("redirect_uris")
    val redirectUris: List<String>,
    val scope: String,
    @SerialName("dpop_bound_access_tokens")
    val dpopBoundAccessTokens: Boolean,
    @SerialName("token_endpoint_auth_method")
    val tokenEndpointAuthMethod: String,
    @SerialName("token_endpoint_auth_signing_alg")
    val tokenEndpointAuthSigningAlg: String,
    @SerialName("client_name")
    val clientName: String? = null,
    @SerialName("client_uri")
    val clientUri: String? = null,
    @SerialName("logo_uri")
    val logoUri: String? = null,
    @SerialName("tos_uri")
    val tosUri: String? = null,
    @SerialName("policy_uri")
    val policyUri: String? = null,
    @SerialName("jwks_uri")
    val jwksUri: String
)
package dev.schlaubi.openid.helper.util

import dev.schlaubi.openid.helper.providers.RequestInterceptorBuilder
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.util.*

fun RequestInterceptorBuilder.RequestContext<*>.validateClientSecret(clientId: String, clientSecret: String) {
    val header = call.request.parseAuthorizationHeader()
    if (header is HttpAuthHeader.Single) {
        if (header.authScheme.equals("Basic", ignoreCase = true)) {
            if (header.blob == "$clientId:$clientSecret".encodeBase64()) return
        }
    }
    if (data is Parameters) {
        if (clientId != data["client_id"]) throw BadRequestException("Invalid client credentials")
        if (clientSecret != data["client_secret"]) throw BadRequestException("Invalid client credentials")
    }
}
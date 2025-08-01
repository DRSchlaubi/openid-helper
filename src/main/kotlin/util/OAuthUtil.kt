package dev.schlaubi.openid.helper.util

import dev.schlaubi.openid.helper.providers.RequestInterceptorBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.http.ParametersBuilder
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.parseAuthorizationHeader
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun RequestInterceptorBuilder.useHeaderForOAuthClientCredentials(formBodyBuilder: ParametersBuilder.() -> Unit = {}) {
    formBody { (form, _, request) ->
        request.basicAuth(form["client_id"]!!, form["client_secret"]!!)
        remove("client_id")
        remove("client_secret")
        formBodyBuilder()
    }
}

fun RequestInterceptorBuilder.fixLowercaseBearer() {
    json { (_, call, request) ->
        request.bearerAuth((call.request.parseAuthorizationHeader() as HttpAuthHeader.Single).blob)
    }
}

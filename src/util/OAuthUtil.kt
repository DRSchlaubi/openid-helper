package dev.schlaubi.openid.helper.util

import dev.schlaubi.openid.helper.providers.InterceptorBuilder
import io.ktor.client.request.basicAuth
import io.ktor.http.ParametersBuilder
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
fun InterceptorBuilder.useHeaderForOAuthClientCredentials(formBodyBuilder: ParametersBuilder.() -> Unit = {}) {
    formBody({ form, _ -> basicAuth(form["client_id"]!!, form["client_secret"]!!) }, {
        remove("client_id")
        remove("client_secret")
        formBodyBuilder()
    })
}

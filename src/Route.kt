package dev.schlaubi.openid.helper

import dev.schlaubi.openid.helper.models.OpenIDManifest
import dev.schlaubi.openid.helper.providers.Provider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.resources.get
import io.ktor.server.resources.handle
import io.ktor.server.resources.resource
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.method
import io.ktor.server.util.url
import io.ktor.util.filter

val LOG = KotlinLogging.logger { }
val PROXY_HEADERS = listOf(HttpHeaders.Host)

val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json()
    }
    install(ContentEncoding) {
        gzip()
    }
}

fun Route.oauthRoutes() {
    get<ProviderRoute.OpenIDConfiguration> {
        val provider = it.provider
        val name = provider.name
        val application = call.application

        call.respond(
            OpenIDManifest(
                application.fullHref(it.parent),
                application.fullHref(ProviderRoute.Authorize(name)),
                application.fullHref(ProviderRoute.Token(name)),
                application.fullHref(ProviderRoute.UserInfo(name)),
                provider.jwksEndpoint
            )
        )
    }

    get<ProviderRoute.Authorize> {
        val url = call.url {
            it.provider.authorize(this, call)
        }

        call.respondRedirect(url)
    }

    proxyRoute<ProviderRoute.Token>(HttpMethod.Post, Provider::token)
    proxyRoute<ProviderRoute.UserInfo>(HttpMethod.Get, Provider::userEndpoint)
}


inline fun <reified R : HasProvider> Route.proxyRoute(
    method: HttpMethod,
    crossinline selectInterceptor: Provider.() -> Provider.RouteInterceptor
) = resource<R> {
    method(method) {
        handle<R> {
            val provider = it.provider
            val interceptor = provider.selectInterceptor()
            val response = httpClient.request(call.url {
                interceptor.request.urlUpdater(this, call)
            }) {
                this.method = method
                intercept(call, interceptor.request)
                headers.appendAll(call.request.headers.filter { name, _ -> name !in PROXY_HEADERS && name !in headers })
            }

            LOG.info { "Forwarded request to ${response.request.url}, response: ${response.status}" }

            val body = response.interceptBody(interceptor.response)
            call.response.status(response.status)
            call.response.header(HttpHeaders.ContentType, response.contentType().toString())
            if (interceptor.response.typeInfo != null) {
                call.respond(body, interceptor.response.typeInfo!!)
            } else {
                call.response.header(HttpHeaders.ContentLength, response.contentLength().toString())
                call.respond(body)
            }
        }
    }
}

suspend fun <T> HttpResponse.interceptBody(interceptor: Provider.Interceptor<T>): Any = with(interceptor) {
    val body = receiveBody()
    return body.toResult()
}

suspend fun <T> HttpRequestBuilder.intercept(call: ApplicationCall, interceptor: Provider.Interceptor<T>) =
    with(interceptor) {
        val body = if (method != HttpMethod.Get) call.receiveBody() else interceptor.emptyBody()
        requestUpdater(body, call)
        if (method != HttpMethod.Get) {
            setBody(body.toResult())
        }
    }

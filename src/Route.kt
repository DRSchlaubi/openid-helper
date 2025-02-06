package dev.schlaubi.openid.helper

import dev.schlaubi.openid.helper.models.OpenIDManifest
import dev.schlaubi.openid.helper.providers.Provider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import tech.relaycorp.doh.DoHClient
import kotlin.time.Duration.Companion.seconds

val LOG = KotlinLogging.logger { }
val PROXY_HEADERS =
    listOf(HttpHeaders.Host, HttpHeaders.XForwardedFor, HttpHeaders.XForwardedProto, HttpHeaders.XForwardedHost)

private val json = Json {
    ignoreUnknownKeys = true
}

val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(json)
    }
    install(ContentEncoding) {
        gzip()
    }

    install(HttpRequestRetry) {
        maxRetries = 0
    }

    expectSuccess = true

    install(HttpTimeout) {
        requestTimeoutMillis = 5.seconds.inWholeMilliseconds
    }
}
val dnsClient = DoHClient()

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

            val body =
                if (response.status.isSuccess()) response.interceptBody(interceptor.response) else response.bodyAsChannel()
            call.response.status(response.status)
            call.response.header(HttpHeaders.ContentType, response.contentType().toString())
            if (response.status.isSuccess() && interceptor.response.typeInfo != null) {
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
    return body.toResult(this@interceptBody)
}

suspend fun <T> HttpRequestBuilder.intercept(call: ApplicationCall, interceptor: Provider.Interceptor<T>) =
    with(interceptor) {
        val body = if (method != HttpMethod.Get) call.receiveBody() else interceptor.emptyBody()
        val newBody = body.toResult(call, this@intercept)
        if (method != HttpMethod.Get) {
            setBody(newBody)
        }
    }

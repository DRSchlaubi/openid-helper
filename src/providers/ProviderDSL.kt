@file:OptIn(ExperimentalContracts::class)

package dev.schlaubi.openid.helper.providers

import dev.schlaubi.openid.helper.providers.Provider.RouteInterceptor
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.takeFrom
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@DslMarker
annotation class ProviderDSL

@ProviderDSL
class ProviderBuilder(private val name: String) {
    private var authorize: URLUpdater = { }
    private var token: RouteInterceptor = RouteInterceptor.Forward
    private var userEndpoint: RouteInterceptor = RouteInterceptor.Forward
    var jwksEndpoint: String? = null
    var useOauth1a: Boolean = false

    fun authorize(block: URLUpdater) {
        authorize = block
    }

    fun authorize(url: String) {
        authorize { takeFrom(url) }
    }

    fun token(block: RouteInterceptorBuilder.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        token = routeInterceptor(block)
    }

    fun userEndpoint(block: RouteInterceptorBuilder.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        userEndpoint = routeInterceptor(block)
    }

    fun userEndpoint(url: String) = userEndpoint {
        request {
            url(url)
        }
    }

    fun token(url: String) = token {
        request {
            url(url)
        }
    }

    fun build(): Provider = Provider(name, authorize, useOauth1a, token, userEndpoint, jwksEndpoint)
}

@ProviderDSL
class RouteInterceptorBuilder {
    private var requestInterceptor: Provider.Interceptor<*> = Provider.NoopInterceptor
    private var responseInterceptor: Provider.Interceptor<*> = Provider.NoopInterceptor

    fun request(block: InterceptorBuilder.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        requestInterceptor = InterceptorBuilder().apply(block).build()
    }

    fun response(block: InterceptorBuilder.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        responseInterceptor = InterceptorBuilder().apply(block).build()
    }

    fun build() = RouteInterceptor(requestInterceptor, responseInterceptor)
}

@ProviderDSL
class InterceptorBuilder {
    private var interceptor: Provider.Interceptor<*> = Provider.NoopInterceptor
    private var urlUpdater: URLUpdater = {}

    fun url(block: URLUpdater) {
        urlUpdater = block
    }

    fun url(url: String) = url { takeFrom(url) }

    fun json(
        requestUpdater: RequestUpdater<JsonObject> = { _, _ -> },
        builder: JsonObjectBuilder.(JsonObject) -> Unit
    ) {
        interceptor = Provider.JsonInterceptor(builder, urlUpdater, requestUpdater)
    }

    fun formBody(requestUpdater: RequestUpdater<Parameters> = { _, _ -> }, builder: ParametersBuilder.() -> Unit) {
        interceptor = Provider.FormInterceptor(builder, urlUpdater, requestUpdater)
    }

    fun build() = interceptor.with(urlUpdater)
}

inline fun provider(name: String, block: ProviderBuilder.() -> Unit): Provider {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return ProviderBuilder(name).apply(block).build()
}

inline fun routeInterceptor(block: RouteInterceptorBuilder.() -> Unit): RouteInterceptor {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return RouteInterceptorBuilder().apply(block).build()
}

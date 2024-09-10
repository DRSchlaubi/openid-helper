package dev.schlaubi.openid.helper.providers

import dev.kord.cache.api.DataCache
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.Route
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject

typealias URLUpdater = suspend URLBuilder.(ApplicationCall) -> Unit

typealias RouteBuilder = Route.() -> Unit

typealias Coroutine = suspend CoroutineScope.() -> Unit

data class Provider(
    val name: String,
    val authorize: URLUpdater,
    val oauth1a: Boolean,
    val token: RouteInterceptor,
    val userEndpoint: RouteInterceptor,
    val jwksEndpoint: String? = null,
    val additionalRoutes: RouteBuilder? = null,
    val register: Coroutine? = null,
) {
    data class RouteInterceptor(
        val request: Interceptor<*>,
        val response: Interceptor<*>,
    ) {
        companion object {
            val Forward = RouteInterceptor(NoopInterceptor, NoopInterceptor)
        }
    }

    interface Interceptor<T> {
        val urlUpdater: URLUpdater
        val typeInfo: TypeInfo? get() = null

        suspend fun ApplicationCall.receiveBody(): T
        suspend fun HttpResponse.receiveBody(): T

        suspend fun T.toResult(call: ApplicationCall, request: HttpRequestBuilder): Any
        suspend fun T.toResult(response: HttpResponse): Any

        fun emptyBody(): T

        fun with(
            urlUpdater: URLUpdater = this.urlUpdater,
        ): Interceptor<T>
    }

    open class NoopInterceptor(
        override val urlUpdater: URLUpdater = { },
    ) : Interceptor<ByteReadChannel> {
        override suspend fun ApplicationCall.receiveBody(): ByteReadChannel = receiveChannel()
        override suspend fun HttpResponse.receiveBody(): ByteReadChannel = bodyAsChannel()

        override suspend fun ByteReadChannel.toResult(call: ApplicationCall, request: HttpRequestBuilder): Any = this
        override suspend fun ByteReadChannel.toResult(response: HttpResponse): Any = this
        override fun emptyBody(): ByteReadChannel = ByteReadChannel(byteArrayOf())

        override fun with(
            urlUpdater: URLUpdater,
        ) = NoopInterceptor(urlUpdater = urlUpdater)

        companion object : NoopInterceptor()
    }

    data class TextInterceptor(
        override val typeInfo: TypeInfo,
        private val requestBuilder: suspend (ByteReadChannel, ApplicationCall, HttpRequestBuilder) -> Any,
        private val responseBuilder: suspend (ByteReadChannel, HttpResponse) -> Any,
        override val urlUpdater: URLUpdater = { },
    ) : Interceptor<ByteReadChannel> {
        override suspend fun ApplicationCall.receiveBody(): ByteReadChannel = receiveChannel()
        override suspend fun HttpResponse.receiveBody(): ByteReadChannel = bodyAsChannel()

        override suspend fun ByteReadChannel.toResult(call: ApplicationCall, request: HttpRequestBuilder): Any =
            requestBuilder(this, call, request)

        override suspend fun ByteReadChannel.toResult(response: HttpResponse): Any = responseBuilder(this, response)
        override fun emptyBody(): ByteReadChannel = ByteReadChannel(byteArrayOf())

        override fun with(
            urlUpdater: URLUpdater,
        ) = copy(urlUpdater = urlUpdater)

        companion object : NoopInterceptor()
    }

    data class FormInterceptor(
        private val requestBuilder: ParametersBuilder.(Parameters, ApplicationCall, HttpRequestBuilder) -> Unit,
        private val responseBuilder: ParametersBuilder.(Parameters, HttpResponse) -> Unit,
        override val urlUpdater: URLUpdater,
    ) : Interceptor<Parameters> {
        override suspend fun ApplicationCall.receiveBody(): Parameters = receiveParameters()
        override suspend fun HttpResponse.receiveBody(): Parameters = body()

        override suspend fun Parameters.toResult(call: ApplicationCall, request: HttpRequestBuilder): Any =
            toResult body@{
                requestBuilder.invoke(this, this@toResult, call, request)
            }

        override suspend fun Parameters.toResult(response: HttpResponse): Any = toResult body@{
            responseBuilder.invoke(this, this@toResult, response)
        }

        private suspend fun Parameters.toResult(block: suspend ParametersBuilder.() -> Unit): OutgoingContent {
            val parameters = Parameters.build {
                appendAll(this@toResult)
                block()
            }

            return FormDataContent(parameters)
        }

        override fun emptyBody(): Parameters = Parameters.Empty
        override fun with(urlUpdater: URLUpdater): Interceptor<Parameters> =
            copy(urlUpdater = urlUpdater)
    }

    data class JsonInterceptor(
        private val requestBuilder: JsonObjectBuilder.(JsonObject, ApplicationCall, HttpRequestBuilder) -> Unit,
        private val responseBuilder: suspend JsonObjectBuilder.(JsonObject, HttpResponse) -> Unit,
        override val urlUpdater: URLUpdater,
    ) : Interceptor<JsonObject> {
        override val typeInfo: TypeInfo = typeInfo<JsonObject>()
        override suspend fun ApplicationCall.receiveBody(): JsonObject = receive()
        override suspend fun HttpResponse.receiveBody(): JsonObject {
            val contentType = contentType()
            return if (contentType == ContentType.Application.Json) {
                body()
            } else {
                Json.decodeFromString(bodyAsText())
            }
        }

        override suspend fun JsonObject.toResult(call: ApplicationCall, request: HttpRequestBuilder): Any =
            toResult body@{
                requestBuilder.invoke(this, this@toResult, call, request)
            }

        override suspend fun JsonObject.toResult(response: HttpResponse): Any = toResult body@{
            responseBuilder.invoke(this, this@toResult, response)
        }

        private suspend fun JsonObject.toResult(block: suspend JsonObjectBuilder.() -> Unit): JsonObject {
            val elements = buildJsonObject {
                block(this)
            }

            return JsonObject(this + elements)
        }

        override fun emptyBody(): JsonObject = JsonObject(emptyMap())

        override fun with(urlUpdater: URLUpdater) =
            copy(urlUpdater = urlUpdater)
    }
}

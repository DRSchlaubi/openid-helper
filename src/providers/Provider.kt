package dev.schlaubi.openid.helper.providers

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveParameters
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject

typealias URLUpdater = URLBuilder.(ApplicationCall) -> Unit
typealias RequestUpdater<T> = HttpRequestBuilder.(T, ApplicationCall) -> Unit

data class Provider(
    val name: String,
    val authorize: URLUpdater,
    val oauth1a: Boolean,
    val token: RouteInterceptor,
    val userEndpoint: RouteInterceptor,
    val jwksEndpoint: String? = null
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
        val requestUpdater: RequestUpdater<T>
        val typeInfo: TypeInfo? get() = null

        suspend fun ApplicationCall.receiveBody(): T
        suspend fun HttpResponse.receiveBody(): T

        suspend fun T.toResult(): Any

        fun emptyBody(): T

        fun with(
            urlUpdater: URLUpdater = this.urlUpdater,
            requestUpdater: RequestUpdater<T> = this.requestUpdater
        ): Interceptor<T>
    }

    open class NoopInterceptor(
        override val urlUpdater: URLUpdater = { },
        override val requestUpdater: RequestUpdater<ByteReadChannel> = { _, _ -> }
    ) : Interceptor<ByteReadChannel> {
        override suspend fun ApplicationCall.receiveBody(): ByteReadChannel = receiveChannel()
        override suspend fun HttpResponse.receiveBody(): ByteReadChannel = bodyAsChannel()

        override suspend fun ByteReadChannel.toResult(): Any = this
        override fun emptyBody(): ByteReadChannel = ByteReadChannel(byteArrayOf())

        override fun with(
            urlUpdater: URLUpdater,
            requestUpdater: RequestUpdater<ByteReadChannel>
        ) = NoopInterceptor(urlUpdater = urlUpdater, requestUpdater = requestUpdater)

        companion object : NoopInterceptor()
    }

    data class FormInterceptor(
        private val block: ParametersBuilder.() -> Unit,
        override val urlUpdater: URLUpdater,
        override val requestUpdater: RequestUpdater<Parameters>
    ) : Interceptor<Parameters> {
        override suspend fun ApplicationCall.receiveBody(): Parameters = receiveParameters()
        override suspend fun HttpResponse.receiveBody(): Parameters = body()
        override suspend fun Parameters.toResult(): OutgoingContent {
            val parameters = Parameters.build {
                appendAll(this@toResult)
                block()
            }

            return FormDataContent(parameters)
        }

        override fun emptyBody(): Parameters = Parameters.Empty

        override fun with(urlUpdater: URLUpdater, requestUpdater: RequestUpdater<Parameters>) =
            copy(urlUpdater = urlUpdater, requestUpdater = requestUpdater)
    }

    data class JsonInterceptor(
        private val block: JsonObjectBuilder.(JsonObject) -> Unit,
        override val urlUpdater: URLUpdater,
        override val requestUpdater: RequestUpdater<JsonObject>
    ) : Interceptor<JsonObject> {
        override val typeInfo: TypeInfo = typeInfo<JsonObject>()
        override suspend fun ApplicationCall.receiveBody(): JsonObject = receive()
        override suspend fun HttpResponse.receiveBody(): JsonObject = body()
        override suspend fun JsonObject.toResult(): Any {
            val elements = buildJsonObject {
                block(this@toResult)
            }

            return JsonObject(elements)
        }

        override fun emptyBody(): JsonObject = JsonObject(emptyMap())

        override fun with(urlUpdater: URLUpdater, requestUpdater: RequestUpdater<JsonObject>) =
            copy(urlUpdater = urlUpdater, requestUpdater = requestUpdater)
    }
}

package dev.schlaubi.openid.helper

import com.auth0.jwt.exceptions.JWTVerificationException
import com.nimbusds.jose.JOSEException
import dev.schlaubi.openid.helper.providers.implementations.bluesky.blueskyRoute
import dev.schlaubi.openid.helper.providers.providers
import dev.schlaubi.openid.helper.util.provider
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

suspend fun main() {
    val providers = providers()

    val server = embeddedServer(Netty, port = Config.PORT, host = Config.HOST) {
        val json = Json {
            serializersModule = SerializersModule {
                contextual(JsonPrimitive.serializer())
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Resources)

        install(StatusPages) {
            exception<JWTVerificationException> { call, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<JOSEException> { call, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

        routing {
            oauthRoutes()
            blueskyRoute()
            providers.forEach { (_, provider) ->
                provider(provider.name) {
                    provider.additionalRoutes?.invoke(this)
                }
            }
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
        httpClient.close()
    })

    server.start(wait = true)
}

package dev.schlaubi.openid.helper

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun main() {
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

        routing { oauthRoutes() }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
        httpClient.close()
    })

    server.start(wait = true)
}

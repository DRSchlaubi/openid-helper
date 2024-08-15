package dev.schlaubi.openid.helper

import dev.schlaubi.openid.helper.providers.implementations.mastodon.loadClients
import dev.schlaubi.openid.helper.providers.implementations.mastodon.mastodon
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun main() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
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

        routing {
            oauthRoutes()
            mastodon()
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
        httpClient.close()
    })

    runBlocking {
        loadClients()
    }

    server.start(wait = true)
}

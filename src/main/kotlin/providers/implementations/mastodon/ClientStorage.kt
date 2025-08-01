@file:OptIn(ExperimentalSerializationApi::class)

package dev.schlaubi.openid.helper.providers.implementations.mastodon

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink

private lateinit var clients: MutableMap<String, MastodonOAuthApplication>
private val lock = Mutex()

private val saveFile = Path("storage/mastodon_clients.json")

suspend fun addClient(url: String, client: MastodonOAuthApplication) = lock.withLock {
    clients[url] = client
    if (!SystemFileSystem.exists(saveFile.parent!!)) {
        SystemFileSystem.createDirectories(saveFile.parent!!)
    }
    SystemFileSystem.sink(saveFile).buffered().use {
        Json.encodeToSink(clients, it)
    }
}

suspend fun loadClients() = lock.withLock {
    clients = if (SystemFileSystem.exists(saveFile)) {
        SystemFileSystem.source(saveFile).buffered().use {
            Json.decodeFromSource(it)
        }
    } else {
        mutableMapOf()
    }
}

fun getClient(url: String) = clients[url]

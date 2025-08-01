package dev.schlaubi.openid.helper.util

import dev.kord.cache.api.data.description
import dev.kord.cache.api.delegate.DelegatingDataCache
import dev.kord.cache.api.query
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.redis.RedisConfiguration
import dev.kord.cache.redis.RedisEntryCache
import dev.schlaubi.openid.helper.Config
import io.lettuce.core.RedisClient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

interface State {
    val id: String
}

@Suppress("UNCHECKED_CAST")
val <T : State> KClass<T>.idProperty
    get() = declaredMemberProperties.first { it.name == "id" } as KProperty1<T, String>

suspend inline fun <reified T : State> registerState() {
    cache.register(description(T::class.idProperty))
}

@OptIn(ExperimentalSerializationApi::class)
private val redisConfig by lazy {
    RedisConfiguration {
        client = RedisClient.create(Config.REDIS_URL)

        binaryFormat = ProtoBuf {
            encodeDefaults = false
        }
        reuseConnection = true
    }
}

val cache = DelegatingDataCache {
    default { cache, description ->
        if (Config.REDIS_URL != null) {
            RedisEntryCache(cache, description, redisConfig)
        } else {
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }
    }
}

suspend inline fun <reified T : State> findAndRemoveState(id: String?): T? {
    if (id == null) return null
    val query = cache.query<T> {
        T::class.idProperty eq id
    }
    return query.singleOrNull()?.also { query.remove() }
}

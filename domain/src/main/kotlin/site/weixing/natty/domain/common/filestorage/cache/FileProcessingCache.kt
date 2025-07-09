package site.weixing.natty.domain.common.filestorage.cache

import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime

/**
 * 文件处理结果缓存
 * 用于缓存文件处理的中间结果和最终结果，提升性能
 */
class FileProcessingCache {
    data class CacheEntry<T>(
        val value: T,
        val cachedAt: LocalDateTime = LocalDateTime.now(),
        val expiresAt: LocalDateTime? = null
    )

    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()

    fun <T : Any> put(key: String, value: T, expiresAt: LocalDateTime? = null) {
        cache[key] = CacheEntry(value, expiresAt = expiresAt)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (entry.expiresAt != null && entry.expiresAt.isBefore(LocalDateTime.now())) {
            cache.remove(key)
            return null
        }
        return entry.value as? T
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size
} 
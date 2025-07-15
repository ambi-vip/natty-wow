package site.weixing.natty.server.redis

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration


@Configuration
class RedisConfig {


    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val cacheConfiguration: RedisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer<String?>(StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer<Any?>(
                    JdkSerializationRedisSerializer()
                )
            )

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(connectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build()
    }

}
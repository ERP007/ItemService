package com.fallguys.itemservice.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                     @Value("${item.cache.detail.ttl:20m}") Duration itemDetailTtl) {
        RedisCacheConfiguration itemDetailConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(itemDetailTtl)
                .disableCachingNullValues()
                .disableKeyPrefix()
                .serializeValuesWith(SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer.builder().build()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(itemDetailConfig)
                .withInitialCacheConfigurations(Map.of(ItemCacheNames.ITEM_DETAIL, itemDetailConfig))
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(false);
    }
}

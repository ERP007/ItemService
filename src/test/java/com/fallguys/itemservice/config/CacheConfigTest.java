package com.fallguys.itemservice.config;

import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.ItemView;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheConfigTest {

    @Test
    void itemDetailSerializerReadsItemViewInsteadOfMap() {
        ItemView itemView = itemView();
        RedisSerializer<ItemView> serializer = CacheConfig.itemDetailValueSerializer();

        ItemView decoded = serializer.deserialize(serializer.serialize(itemView));

        assertEquals(itemView, decoded);
    }

    @Test
    void itemDetailSerializerReadsExistingGenericJsonCacheEntry() {
        ItemView itemView = itemView();
        byte[] genericJson = GenericJacksonJsonRedisSerializer.builder().build().serialize(itemView);

        ItemView decoded = CacheConfig.itemDetailValueSerializer().deserialize(genericJson);

        assertEquals(itemView, decoded);
    }

    private static ItemView itemView() {
        return new ItemView(
                "HMC-EN-00214",
                "Engine oil filter",
                "ENGINE_LUBRICATION",
                "Lubrication",
                "ENGINE",
                "Engine",
                ItemUnit.EA,
                120,
                15000,
                true,
                Instant.parse("2026-06-06T10:30:00Z"),
                Instant.parse("2026-06-07T10:30:00Z")
        );
    }
}

package com.eagle.idgenerator.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UuidIdGenerator} 单元测试（基于 uuid-creator 的 UUID v7）。
 */
@DisplayName("UuidIdGenerator")
class UuidIdGeneratorTest {

    private final UuidIdGenerator generator = new UuidIdGenerator();

    @Test
    @DisplayName("nextIdStr should return 32-char hex without hyphens")
    void shouldReturn32CharHex() {
        String id = generator.nextIdStr();
        assertEquals(32, id.length(), "UUID v7 string should be 32 chars without hyphens");
        assertTrue(id.matches("[0-9a-f]{32}"), "UUID v7 string should be lowercase hex: " + id);
    }

    @Test
    @DisplayName("nextUuid should return UUID with version 7")
    void shouldReturnVersion7() {
        UUID uuid = generator.nextUuid();
        assertEquals(7, uuid.version(), "Generated UUID must be version 7 (time-ordered Unix Epoch)");
    }

    @Test
    @DisplayName("should generate unique uuids when called 1000 times")
    void shouldGenerateUniqueUuids() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(generator.nextIdStr());
        }
        assertEquals(1000, ids.size(), "All 1000 UUIDs must be unique");
    }

    @Test
    @DisplayName("nextId should return long value (high 64 bits)")
    void shouldReturnLong() {
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        assertTrue(id1 != id2 || id1 == 0L, "Two consecutive ids should not collide trivially");
    }
}

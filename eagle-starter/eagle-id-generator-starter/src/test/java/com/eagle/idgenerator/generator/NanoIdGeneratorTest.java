package com.eagle.idgenerator.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link NanoIdGenerator} 单元测试（基于 Hutool {@code IdUtil.nanoId}）。
 */
@DisplayName("NanoIdGenerator")
class NanoIdGeneratorTest {

    @Test
    @DisplayName("default size should be 21")
    void defaultSizeShouldBe21() {
        NanoIdGenerator gen = new NanoIdGenerator();
        assertEquals(21, gen.nextId().length());
    }

    @Test
    @DisplayName("custom default size should be respected")
    void customDefaultSizeShouldBeRespected() {
        NanoIdGenerator gen = new NanoIdGenerator(8);
        assertEquals(8, gen.nextId().length());
    }

    @Test
    @DisplayName("nextId(size) should produce given length")
    void shouldProduceGivenLength() {
        NanoIdGenerator gen = new NanoIdGenerator();
        assertEquals(16, gen.nextId(16).length());
    }

    @Test
    @DisplayName("should reject non-positive size")
    void shouldRejectNonPositiveSize() {
        NanoIdGenerator gen = new NanoIdGenerator();
        assertThrows(IllegalArgumentException.class, () -> gen.nextId(0));
        assertThrows(IllegalArgumentException.class, () -> new NanoIdGenerator(-1));
    }

    @Test
    @DisplayName("should generate URL-safe characters only")
    void shouldGenerateUrlSafeChars() {
        NanoIdGenerator gen = new NanoIdGenerator();
        String id = gen.nextId();
        assertTrue(id.matches("[A-Za-z0-9_-]+"),
                "NanoId must contain only URL-safe chars (A-Z, a-z, 0-9, _, -): " + id);
    }

    @Test
    @DisplayName("should generate unique NanoIds when called 1000 times")
    void shouldGenerateUnique() {
        NanoIdGenerator gen = new NanoIdGenerator();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(gen.nextId());
        }
        assertEquals(1000, ids.size());
    }
}

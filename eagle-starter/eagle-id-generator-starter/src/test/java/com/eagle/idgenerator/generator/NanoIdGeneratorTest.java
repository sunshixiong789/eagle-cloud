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
    @DisplayName("默认大小应Be21")
    void defaultSizeShouldBe21() {
        NanoIdGenerator gen = new NanoIdGenerator();
        assertEquals(21, gen.nextId().length());
    }

    @Test
    @DisplayName("自定义默认大小应生效")
    void customDefaultSizeShouldBeRespected() {
        NanoIdGenerator gen = new NanoIdGenerator(8);
        assertEquals(8, gen.nextId().length());
    }

    @Test
    @DisplayName("应生成指定长度")
    void shouldProduceGivenLength() {
        NanoIdGenerator gen = new NanoIdGenerator();
        assertEquals(16, gen.nextId(16).length());
    }

    @Test
    @DisplayName("应拒绝非正数大小")
    void shouldRejectNonPositiveSize() {
        NanoIdGenerator gen = new NanoIdGenerator();
        assertThrows(IllegalArgumentException.class, () -> gen.nextId(0));
        assertThrows(IllegalArgumentException.class, () -> new NanoIdGenerator(-1));
    }

    @Test
    @DisplayName("应生成URL 安全字符")
    void shouldGenerateUrlSafeChars() {
        NanoIdGenerator gen = new NanoIdGenerator();
        String id = gen.nextId();
        assertTrue(id.matches("[A-Za-z0-9_-]+"),
                "NanoId must contain only URL-safe chars (A-Z, a-z, 0-9, _, -): " + id);
    }

    @Test
    @DisplayName("应生成唯一值")
    void shouldGenerateUnique() {
        NanoIdGenerator gen = new NanoIdGenerator();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(gen.nextId());
        }
        assertEquals(1000, ids.size());
    }
}

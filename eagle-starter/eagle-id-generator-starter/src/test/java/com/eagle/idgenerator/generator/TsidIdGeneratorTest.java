package com.eagle.idgenerator.generator;

import com.github.f4b6a3.tsid.Tsid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TsidIdGenerator} 单元测试（基于 tsid-creator）。
 */
@DisplayName("TSIDIdGenerator")
class TsidIdGeneratorTest {

    private final TsidIdGenerator generator = new TsidIdGenerator(1, 10);

    @Test
    @DisplayName("应返回13 位字符串")
    void shouldReturn13CharString() {
        String id = generator.nextIdStr();
        assertEquals(13, id.length(), "TSID string should be 13 chars: " + id);
        assertTrue(id.matches("[0-9A-Z]{13}"), "TSID string should be uppercase Crockford Base32: " + id);
    }

    @Test
    @DisplayName("应返回正数长")
    void shouldReturnPositiveLong() {
        long id = generator.nextId();
        assertTrue(id > 0, "TSID long should be positive: " + id);
    }

    @Test
    @DisplayName("应生成唯一 TSID")
    void shouldGenerateUniqueTsids() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(generator.nextIdStr());
        }
        assertEquals(1000, ids.size(), "All 1000 TSIDs must be unique");
    }

    @Test
    @DisplayName("使用Instant时应返回TSID")
    void shouldReturnTsidWithInstant() {
        Tsid tsid = generator.nextTsid();
        assertNotNull(tsid);
        assertNotNull(tsid.getInstant(), "TSID must carry an embedded instant");
    }
}

package com.eagle.auth.core.domain.model;

import com.eagle.auth.core.domain.model.enums.BlacklistType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlacklistTest {

    @Test
    @DisplayName("应使用必填字段创建")
    void shouldCreateWithRequiredFields() {
        Blacklist b = Blacklist.create(BlacklistType.PHONE, "13800138000",
                "test", null, 99L, "admin");
        assertEquals(BlacklistType.PHONE, b.getType());
        assertEquals("13800138000", b.getValue());
        assertEquals("test", b.getReason());
        assertEquals(99L, b.getOperatorId());
    }

    @Test
    @DisplayName("应识别已过期记录")
    void shouldDetectExpired() {
        Blacklist b = Blacklist.create(BlacklistType.IP, "1.1.1.1", null,
                LocalDateTime.now().minusMinutes(1), null, null);
        assertTrue(b.isExpired(LocalDateTime.now()));
    }

    @Test
    @DisplayName("过期时间为空时应视为永久有效")
    void shouldTreatNullExpiresAsPermanent() {
        Blacklist b = Blacklist.create(BlacklistType.IP, "1.1.1.1", null,
                null, null, null);
        assertFalse(b.isExpired(LocalDateTime.now().plusYears(10)));
    }
}

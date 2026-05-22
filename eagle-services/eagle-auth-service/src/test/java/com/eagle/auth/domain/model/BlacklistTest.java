package com.eagle.auth.domain.model;

import com.eagle.auth.domain.model.enums.BlacklistType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlacklistTest {

    @Test
    void shouldCreateWithRequiredFields() {
        Blacklist b = Blacklist.create(BlacklistType.PHONE, "13800138000",
                "test", null, 99L, "admin");
        assertEquals(BlacklistType.PHONE, b.getType());
        assertEquals("13800138000", b.getValue());
        assertEquals("test", b.getReason());
        assertEquals(99L, b.getOperatorId());
    }

    @Test
    void shouldDetectExpired() {
        Blacklist b = Blacklist.create(BlacklistType.IP, "1.1.1.1", null,
                LocalDateTime.now().minusMinutes(1), null, null);
        assertTrue(b.isExpired(LocalDateTime.now()));
    }

    @Test
    void shouldTreatNullExpiresAsPermanent() {
        Blacklist b = Blacklist.create(BlacklistType.IP, "1.1.1.1", null,
                null, null, null);
        assertFalse(b.isExpired(LocalDateTime.now().plusYears(10)));
    }
}

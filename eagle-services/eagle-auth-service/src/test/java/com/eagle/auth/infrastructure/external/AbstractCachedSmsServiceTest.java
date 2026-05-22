package com.eagle.auth.infrastructure.external;

import com.eagle.common.exception.ServiceException;
import com.eagle.auth.domain.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 60 秒 putIfAbsent 频控、验证码缓存 hit/miss、校验后失效。
 */
@DisplayName("AbstractCachedSmsService")
class AbstractCachedSmsServiceTest {

    private TestSmsService sms;

    @BeforeEach
    void setUp() {
        sms = new TestSmsService();
    }

    @Test
    @DisplayName("first send invokes provider; second within 60s throws SMS_RATE_LIMIT")
    void rateLimitWithinWindow() {
        sms.sendCode("13800138000");
        assertEquals(1, sms.sendCount.get());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sms.sendCode("13800138000"));
        assertEquals(AuthErrorCode.SMS_RATE_LIMIT, ex.getErrorCode());
        assertEquals(1, sms.sendCount.get(), "provider should not be called again within window");
    }

    @Test
    @DisplayName("verifyCode returns true once and invalidates the code")
    void verifyOnce() {
        sms.sendCode("13800138000");
        String code = sms.lastCode;
        assertTrue(sms.verifyCode("13800138000", code));
        assertFalse(sms.verifyCode("13800138000", code),
                "the same code can not be reused after a successful verify");
    }

    @Test
    @DisplayName("verifyCode returns false for unknown phone or wrong code")
    void verifyMiss() {
        sms.sendCode("13800138000");
        assertFalse(sms.verifyCode("13800138000", "000000"));
        assertFalse(sms.verifyCode("13900139000", sms.lastCode));
    }

    @Test
    @DisplayName("send on different phone is independent")
    void differentPhonesAreIndependent() {
        sms.sendCode("13800138000");
        sms.sendCode("13900139000");
        assertEquals(2, sms.sendCount.get());
    }

    // ====================== stub ======================

    private static final class TestSmsService extends AbstractCachedSmsService {
        final AtomicInteger sendCount = new AtomicInteger();
        volatile String lastCode;

        @Override
        protected boolean isConfigured() {
            return true;
        }

        @Override
        protected void doSend(String phone, String code) {
            sendCount.incrementAndGet();
            this.lastCode = code;
        }

        @Override
        protected String providerName() {
            return "test";
        }
    }
}

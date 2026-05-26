package com.eagle.auth.core.infrastructure.external.provider;

import com.eagle.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockPhoneOneClickProviderTest {

    private final MockPhoneOneClickProvider provider = new MockPhoneOneClickProvider();

    @Test
    @DisplayName("name 返回 mock")
    void shouldExposeMockName() {
        assertEquals("mock", provider.name());
    }

    @Nested
    @DisplayName("verifyAndGetPhone")
    class VerifyAndGetPhone {

        @Test
        @DisplayName("合法手机号格式直接返回")
        void shouldReturnTokenWhenValidPhone() {
            assertEquals("13812345678", provider.verifyAndGetPhone("13812345678"));
        }

        @Test
        @DisplayName("非合法手机号格式抛出领域异常")
        void shouldRejectWhenTokenIsNotPhone() {
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("not-a-phone"));
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("12345678901"));
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("1381234567"));
        }
    }
}

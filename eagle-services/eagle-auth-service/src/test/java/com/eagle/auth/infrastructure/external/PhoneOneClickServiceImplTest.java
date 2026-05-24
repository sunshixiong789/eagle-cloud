package com.eagle.auth.infrastructure.external;

import com.eagle.auth.infrastructure.config.PhoneOneClickProperties;
import com.eagle.auth.infrastructure.external.provider.PhoneOneClickProvider;
import com.eagle.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneOneClickServiceImplTest {

    private static final String EXPECTED_PHONE = "13800138000";

    private PhoneOneClickProperties properties;
    private PhoneOneClickProvider mockProvider;
    private PhoneOneClickProvider aliyunProvider;

    @BeforeEach
    void setUp() {
        properties = new PhoneOneClickProperties();
        mockProvider = new StubProvider("mock", EXPECTED_PHONE);
        aliyunProvider = new StubProvider("aliyun", "13900139000");
    }

    private PhoneOneClickServiceImpl newService() {
        return new PhoneOneClickServiceImpl(properties, List.of(mockProvider, aliyunProvider));
    }

    @Nested
    @DisplayName("verifyAndGetPhone")
    class VerifyAndGetPhone {

        @Test
        @DisplayName("路由到 provider 配置匹配的实现")
        void shouldRouteToConfiguredProvider() {
            properties.setProvider("aliyun");
            String phone = newService().verifyAndGetPhone("any-token");
            assertEquals("13900139000", phone);
        }

        @Test
        @DisplayName("provider 名大小写不敏感")
        void shouldMatchProviderCaseInsensitive() {
            properties.setProvider("Aliyun");
            String phone = newService().verifyAndGetPhone("any-token");
            assertEquals("13900139000", phone);
        }

        @Test
        @DisplayName("enabled=false 时直接拒绝")
        void shouldRejectWhenDisabled() {
            properties.setEnabled(false);
            assertThrows(AppException.class, () -> newService().verifyAndGetPhone("any-token"));
        }

        @Test
        @DisplayName("access_token 为空时拒绝")
        void shouldRejectWhenTokenBlank() {
            properties.setProvider("mock");
            PhoneOneClickServiceImpl service = newService();
            assertThrows(AppException.class, () -> service.verifyAndGetPhone(""));
            assertThrows(AppException.class, () -> service.verifyAndGetPhone(null));
        }

        @Test
        @DisplayName("找不到 provider 时拒绝")
        void shouldRejectWhenProviderUnknown() {
            properties.setProvider("unknown");
            assertThrows(AppException.class, () -> newService().verifyAndGetPhone("any-token"));
        }
    }

    private record StubProvider(String name, String phone) implements PhoneOneClickProvider {
        @Override
        public String verifyAndGetPhone(String accessToken) {
            return phone;
        }
    }
}

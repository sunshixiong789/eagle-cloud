package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import com.eagle.auth.core.infrastructure.config.SmsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 验证短信真实下发的启用条件与审核白名单固定验证码逻辑。
 *
 * <p>{@code provider=hnsls} 且账号/密码/签名齐全时才调用网关，否则回落到
 * {@link AbstractCachedSmsService} 的开发态日志兜底。
 */
@DisplayName("SmsServiceImpl")
class SmsServiceImplTest {

    private static final String MOCK_PHONE = "13800138000";
    private static final String NORMAL_PHONE = "13900139000";
    private static final String MOCK_CODE = "123456";

    private HnslsSmsSender sender;

    @BeforeEach
    void setUp() {
        sender = mock(HnslsSmsSender.class);
    }

    /**
     * 凭据齐全的 hnsls 配置，即 isConfigured() == true 的最小组合。
     */
    private SmsProperties configured() {
        SmsProperties properties = new SmsProperties();
        properties.setProvider(HnslsSmsSender.PROVIDER_NAME);
        properties.setUsername("acct");
        properties.setPassword("secret");
        properties.setSignName("鹰云");
        return properties;
    }

    private SmsServiceImpl service(SmsProperties smsProperties, SmsMockProperties mockProperties) {
        return new SmsServiceImpl(mockProperties, smsProperties, sender);
    }

    private SmsMockProperties whitelist(String... phones) {
        SmsMockProperties properties = new SmsMockProperties();
        properties.setPhones(Set.of(phones));
        return properties;
    }

    @Nested
    @DisplayName("真实下发启用条件")
    class RealDelivery {

        @Test
        @DisplayName("provider=hnsls 且凭据齐全时调用网关下发")
        void sendsThroughGatewayWhenConfigured() {
            service(configured(), new SmsMockProperties()).sendCode(NORMAL_PHONE);

            verify(sender, times(1)).send(eq(NORMAL_PHONE), anyString());
        }

        @Test
        @DisplayName("凭据缺失时不调用网关，回落日志兜底")
        void fallsBackWhenCredentialIncomplete() {
            SmsProperties properties = configured();
            properties.setPassword("");

            service(properties, new SmsMockProperties()).sendCode(NORMAL_PHONE);

            verify(sender, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("provider 非 hnsls 时不调用网关，回落日志兜底")
        void fallsBackWhenProviderUnsupported() {
            SmsProperties properties = configured();
            properties.setProvider("aliyun");

            service(properties, new SmsMockProperties()).sendCode(NORMAL_PHONE);

            verify(sender, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("日志兜底下验证码仍可正常校验，不影响开发联调")
        void fallbackCodeStillVerifiable() {
            SmsServiceImpl service = service(new SmsProperties(), new SmsMockProperties());
            service.sendCode(NORMAL_PHONE);

            assertFalse(service.verifyCode(NORMAL_PHONE, "000000"),
                    "错误验证码不应通过");
        }
    }

    @Nested
    @DisplayName("审核白名单固定验证码")
    class MockWhitelist {

        @Test
        @DisplayName("白名单手机号固定验证码直接校验通过，无需先发送")
        void mockPhoneVerifiesWithFixedCode() {
            SmsServiceImpl service = service(configured(), whitelist(MOCK_PHONE));

            assertTrue(service.verifyCode(MOCK_PHONE, MOCK_CODE));
            // 固定验证码可重复使用（审核人员可能多次登录）
            assertTrue(service.verifyCode(MOCK_PHONE, MOCK_CODE));
        }

        @Test
        @DisplayName("白名单手机号错误验证码校验失败")
        void mockPhoneRejectsWrongCode() {
            assertFalse(service(configured(), whitelist(MOCK_PHONE)).verifyCode(MOCK_PHONE, "654321"));
        }

        @Test
        @DisplayName("白名单手机号不受频控限制，且不真实下发")
        void mockPhoneSkipsRateLimitAndGateway() {
            SmsServiceImpl service = service(configured(), whitelist(MOCK_PHONE));

            service.sendCode(MOCK_PHONE);
            assertDoesNotThrow(() -> service.sendCode(MOCK_PHONE));
            verify(sender, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("固定验证码对非白名单手机号无效")
        void mockCodeRejectedForNormalPhone() {
            SmsServiceImpl service = service(configured(), whitelist(MOCK_PHONE));
            service.sendCode(NORMAL_PHONE);

            assertFalse(service.verifyCode(NORMAL_PHONE, MOCK_CODE));
        }

        @Test
        @DisplayName("白名单为空时固定验证码整体关闭")
        void emptyWhitelistDisablesMock() {
            assertFalse(service(configured(), new SmsMockProperties()).verifyCode(MOCK_PHONE, MOCK_CODE));
        }
    }
}

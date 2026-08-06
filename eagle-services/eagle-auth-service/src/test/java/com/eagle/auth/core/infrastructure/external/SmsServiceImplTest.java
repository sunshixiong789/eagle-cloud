package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证审核白名单固定验证码逻辑：白名单手机号跳过真实发送、固定码直接校验通过。
 * 真实短信发送已下线（{@link SmsServiceImpl#isConfigured()} 恒 false）,
 * 非白名单手机号走 {@link AbstractCachedSmsService} 的开发态日志兜底,不再涉及真实 provider。
 */
@DisplayName("SmsServiceImpl 审核白名单固定验证码")
class SmsServiceImplTest {

    private static final String MOCK_PHONE = "13800138000";
    private static final String NORMAL_PHONE = "13900139000";
    private static final String MOCK_CODE = "123456";

    private SmsServiceImpl smsService;

    @BeforeEach
    void setUp() {
        SmsMockProperties mockProperties = new SmsMockProperties();
        mockProperties.setPhones(Set.of(MOCK_PHONE));

        smsService = new SmsServiceImpl(mockProperties);
    }

    @Test
    @DisplayName("白名单手机号固定验证码直接校验通过，无需先发送")
    void mockPhoneVerifiesWithFixedCode() {
        assertTrue(smsService.verifyCode(MOCK_PHONE, MOCK_CODE));
        // 固定验证码可重复使用（审核人员可能多次登录）
        assertTrue(smsService.verifyCode(MOCK_PHONE, MOCK_CODE));
    }

    @Test
    @DisplayName("白名单手机号错误验证码校验失败")
    void mockPhoneRejectsWrongCode() {
        assertFalse(smsService.verifyCode(MOCK_PHONE, "654321"));
    }

    @Test
    @DisplayName("白名单手机号不受频控限制")
    void mockPhoneSkipsRateLimit() {
        smsService.sendCode(MOCK_PHONE);
        assertDoesNotThrow(() -> smsService.sendCode(MOCK_PHONE));
    }

    @Test
    @DisplayName("非白名单手机号走开发态日志兜底流程，验证码可正常校验")
    void normalPhoneUsesDevLogFallback() {
        smsService.sendCode(NORMAL_PHONE);

        assertFalse(smsService.verifyCode(NORMAL_PHONE, MOCK_CODE),
                "固定验证码对非白名单手机号无效");
    }

    @Test
    @DisplayName("白名单为空时固定验证码整体关闭")
    void emptyWhitelistDisablesMock() {
        SmsMockProperties disabled = new SmsMockProperties();
        SmsServiceImpl service = new SmsServiceImpl(disabled);
        assertFalse(service.verifyCode(MOCK_PHONE, MOCK_CODE));
    }
}

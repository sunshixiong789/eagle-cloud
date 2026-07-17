package com.eagle.auth.core.infrastructure.external;

import com.eagle.message.channel.sms.SmsProvider;
import com.eagle.message.properties.MessageProperties;
import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证审核白名单固定验证码逻辑：白名单手机号跳过真实发送、固定码直接校验通过，
 * 非白名单手机号完全走 {@link AbstractCachedSmsService} 正常流程。
 */
@DisplayName("SmsServiceImpl 审核白名单固定验证码")
class SmsServiceImplTest {

    private static final String MOCK_PHONE = "13800138000";
    private static final String NORMAL_PHONE = "13900139000";
    private static final String MOCK_CODE = "123456";

    private SmsProvider smsProvider;
    private SmsServiceImpl smsService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        smsProvider = mock(SmsProvider.class);
        when(smsProvider.name()).thenReturn("test");
        ObjectProvider<SmsProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(smsProvider);

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getSms().setSignName("测试签名");
        messageProperties.getSms().setTemplateId("TPL-001");

        SmsMockProperties mockProperties = new SmsMockProperties();
        mockProperties.setPhones(Set.of(MOCK_PHONE));

        smsService = new SmsServiceImpl(objectProvider, messageProperties, mockProperties);
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
    @DisplayName("白名单手机号不发送真实短信且不受频控限制")
    void mockPhoneSkipsRealSendAndRateLimit() {
        smsService.sendCode(MOCK_PHONE);
        assertDoesNotThrow(() -> smsService.sendCode(MOCK_PHONE));
        verify(smsProvider, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("非白名单手机号走正常发送与校验流程")
    void normalPhoneUsesRealFlow() {
        smsService.sendCode(NORMAL_PHONE);

        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.captor();
        verify(smsProvider).send(eq(NORMAL_PHONE), eq("TPL-001"), eq("测试签名"), paramsCaptor.capture());
        String realCode = paramsCaptor.getValue().get("code");

        assertFalse(smsService.verifyCode(NORMAL_PHONE, MOCK_CODE),
                "固定验证码对非白名单手机号无效");
        assertTrue(smsService.verifyCode(NORMAL_PHONE, realCode));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("白名单为空时固定验证码整体关闭")
    void emptyWhitelistDisablesMock() {
        SmsMockProperties disabled = new SmsMockProperties();
        SmsServiceImpl service = new SmsServiceImpl(
                mock(ObjectProvider.class), new MessageProperties(), disabled);
        assertFalse(service.verifyCode(MOCK_PHONE, MOCK_CODE));
    }
}

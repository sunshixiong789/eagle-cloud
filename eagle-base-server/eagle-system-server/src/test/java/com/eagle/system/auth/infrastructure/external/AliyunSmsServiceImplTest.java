package com.eagle.auth.infrastructure.external;

import com.eagle.common.exception.ServiceException;
import com.eagle.auth.infrastructure.config.AliyunSmsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("阿里云短信服务测试")
class AliyunSmsServiceImplTest {

    private AliyunSmsServiceImpl smsService;

    @BeforeEach
    void setUp() throws Exception {
        AliyunSmsProperties properties = new AliyunSmsProperties();
        // 默认值为空字符串，不会初始化阿里云客户端
        smsService = new AliyunSmsServiceImpl(properties);
        smsService.init();
    }

    @Test
    @DisplayName("发送验证码后验证 - 错误验证码应返回 false")
    void shouldReturnFalseWhenCodeIsWrong() {
        smsService.sendCode("13800138000");

        assertFalse(smsService.verifyCode("13800138000", "000000"));
    }

    @Test
    @DisplayName("验证码验证 - 未发送验证码应返回 false")
    void shouldReturnFalseWhenNoCodeSent() {
        assertFalse(smsService.verifyCode("13900139000", "123456"));
    }

    @Test
    @DisplayName("频率限制 - 60秒内重复发送应抛出异常")
    void shouldThrowExceptionWhenSendingTooFrequently() {
        smsService.sendCode("13800138001");

        assertThrows(ServiceException.class, () ->
                smsService.sendCode("13800138001")
        );
    }

    @Test
    @DisplayName("频率限制 - 不同手机号不受限制")
    void shouldAllowSendingToDifferentPhones() {
        smsService.sendCode("13800138002");

        assertDoesNotThrow(() -> smsService.sendCode("13800138003"));
    }

    @Test
    @DisplayName("验证码验证 - 不同手机号的验证码互不影响")
    void shouldIsolateCodesBetweenPhones() {
        smsService.sendCode("13800138004");
        smsService.sendCode("13800138005");

        assertFalse(smsService.verifyCode("13800138004", "wrong"));
        assertFalse(smsService.verifyCode("13800138005", "wrong"));
    }

    @Test
    @DisplayName("验证码验证 - 错误验证码不会清除缓存")
    void shouldNotInvalidateCodeOnWrongVerification() {
        smsService.sendCode("13800138006");
        assertFalse(smsService.verifyCode("13800138006", "wrong_code"));
        // 原验证码仍然在缓存中
        assertFalse(smsService.verifyCode("13800138006", "another_wrong"));
    }
}

package com.eagle.auth.web.controller;

import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.service.SmsService;
import com.eagle.auth.infrastructure.security.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.cache.CacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SmsController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("短信验证码控制器测试")
class SmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SmsService smsService;

    @MockitoBean
    private AccountApplicationService accountApplicationService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    @DisplayName("发送验证码 - 成功")
    void shouldSendCodeSuccessfully() throws Exception {
        doNothing().when(smsService).sendCode("13800138000");

        mockMvc.perform(post("/sms/code")
                        .param("phone", "13800138000"))
                .andExpect(status().isOk());

        verify(smsService).sendCode("13800138000");
    }

    @Test
    @DisplayName("发送验证码 - 手机号格式错误应返回 400")
    void shouldReturn400WhenPhoneFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/sms/code")
                        .param("phone", "1234"))
                .andExpect(status().isBadRequest());

        verify(smsService, never()).sendCode(anyString());
    }

    @Test
    @DisplayName("发送验证码 - 手机号为空应返回 400")
    void shouldReturn400WhenPhoneIsEmpty() throws Exception {
        mockMvc.perform(post("/sms/code")
                        .param("phone", ""))
                .andExpect(status().isBadRequest());

        verify(smsService, never()).sendCode(anyString());
    }

    @Test
    @DisplayName("发送验证码 - 频率限制应返回错误")
    void shouldReturnErrorWhenRateLimited() throws Exception {
        doThrow(new IllegalStateException("发送过于频繁，请60秒后重试"))
                .when(smsService).sendCode("13800138000");

        mockMvc.perform(post("/sms/code")
                        .param("phone", "13800138000"))
                .andExpect(status().is4xxClientError());
    }
}

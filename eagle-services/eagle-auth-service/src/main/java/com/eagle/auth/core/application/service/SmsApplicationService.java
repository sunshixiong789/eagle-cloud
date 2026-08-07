package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.service.SmsService;
import com.eagle.auth.core.infrastructure.security.ClientIpHolder;
import com.eagle.auth.core.infrastructure.security.SmsSendRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 短信发送应用服务。
 *
 * <p>承载「发送验证码」用例的编排：先过 {@link SmsSendRateLimiter} 的 IP 级频控，
 * 再走 {@link SmsService} 内部按 phone 的 60 秒频控——两道闸门共同防止同一 IP
 * 用大量手机号刷发短信。
 *
 * <p>频控依赖 Redis 与 {@link ClientIpHolder}（请求线程的 ThreadLocal），属基础设施，
 * 由本应用服务持有；Controller 只做入参校验，不直接触碰。
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class SmsApplicationService {

    private final SmsService smsService;
    private final AccountApplicationService accountApplicationService;
    private final SmsSendRateLimiter smsSendRateLimiter;

    /**
     * 发送登录 / 注册验证码。
     */
    public void sendCode(String phone) {
        smsSendRateLimiter.checkAndIncrement(ClientIpHolder.get());
        smsService.sendCode(phone);
    }

    /**
     * 发送找回密码验证码（手机号未绑定账号时由下游抛出 PHONE_NOT_BOUND）。
     */
    public void sendResetCode(String phone) {
        smsSendRateLimiter.checkAndIncrement(ClientIpHolder.get());
        accountApplicationService.sendResetCode(phone);
    }
}

package com.eagle.auth.interfaces.controller;

import com.eagle.common.exception.codes.DataErrorCode;
import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.service.SmsService;
import com.eagle.auth.infrastructure.security.ClientIpHolder;
import com.eagle.auth.infrastructure.security.SmsSendRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * 短信验证码控制器。
 *
 * <p>所有发送端点必须通过 {@link SmsSendRateLimiter} 做 IP 级频控，再走 SmsService
 * 内部按 phone 的 60 秒频控；避免同一 IP 用大量手机号刷发短信。
 *
 * @author sunshixiong
 */
@Slf4j
@Tag(name = "短信服务", description = "短信验证码发送")
@RestController
@RequestMapping("sms")
@RequiredArgsConstructor
public class SmsController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final SmsService smsService;
    private final AccountApplicationService accountApplicationService;
    private final SmsSendRateLimiter smsSendRateLimiter;

    @Operation(summary = "发送短信验证码", description = "向指定手机号发送验证码（带 IP 级 / 手机号级双重频控）")
    @PostMapping("/code")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> sendCode(@Parameter(description = "手机号", required = true)
                                         @RequestParam String phone) {
        validatePhone(phone);
        smsSendRateLimiter.checkAndIncrement(ClientIpHolder.get());
        smsService.sendCode(phone);
        log.info("sms code sent, phone-suffix={}", maskTail(phone));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "发送找回密码验证码", description = "验证手机号已绑定账号后发送重置密码验证码")
    @PostMapping("/code/reset")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> sendResetCode(@Parameter(description = "手机号", required = true)
                                              @RequestParam String phone) {
        validatePhone(phone);
        smsSendRateLimiter.checkAndIncrement(ClientIpHolder.get());
        accountApplicationService.sendResetCode(phone);
        log.info("sms reset code sent, phone-suffix={}", maskTail(phone));
        return ResponseEntity.ok().build();
    }

    private void validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw DataErrorCode.INVALID_PHONE_FORMAT.toDomainException();
        }
    }

    private String maskTail(String phone) {
        return phone.length() >= 4 ? "***" + phone.substring(phone.length() - 4) : "***";
    }
}

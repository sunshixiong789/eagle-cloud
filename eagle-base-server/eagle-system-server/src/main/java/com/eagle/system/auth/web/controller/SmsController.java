package com.eagle.system.auth.web.controller;

import com.eagle.system.auth.application.service.AccountApplicationService;
import com.eagle.system.auth.domain.service.SmsService;
import com.eagle.common.exception.codes.DataErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码控制器
 *
 * @author sunshixiong
 */
@Tag(name = "短信服务", description = "短信验证码发送")
@RestController
@RequestMapping("sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;
    private final AccountApplicationService accountApplicationService;

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 200 OK
     */
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送验证码")
    @PostMapping("/code")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> sendCode(@Parameter(description = "手机号", required = true) @RequestParam String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw DataErrorCode.INVALID_PHONE_FORMAT.toDomainException();
        }
        smsService.sendCode(phone);
        return ResponseEntity.ok().build();
    }

    /**
     * 发送找回密码验证码（验证手机号已绑定账号后发送）
     *
     * @param phone 手机号
     * @return 200 OK
     */
    @Operation(summary = "发送找回密码验证码", description = "验证手机号已绑定账号后发送重置密码验证码")
    @PostMapping("/code/reset")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> sendResetCode(@Parameter(description = "手机号", required = true) @RequestParam String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw DataErrorCode.INVALID_PHONE_FORMAT.toDomainException();
        }
        accountApplicationService.sendResetCode(phone);
        return ResponseEntity.ok().build();
    }
}

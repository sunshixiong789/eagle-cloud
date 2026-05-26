package com.eagle.auth.core.interfaces.controller;

import com.eagle.common.dto.EagleUser;
import com.eagle.auth.core.application.command.FreezeAccountCommand;
import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.interfaces.dto.request.BindPhoneRequest;
import com.eagle.auth.core.interfaces.dto.request.ChangePasswordRequest;
import com.eagle.auth.core.interfaces.dto.request.CreateAccountRequest;
import com.eagle.auth.core.interfaces.dto.request.FreezeAccountRequest;
import com.eagle.auth.core.interfaces.dto.request.RegisterAccountRequest;
import com.eagle.auth.core.interfaces.dto.request.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 账号管理控制器
 * <p>
 * 所有认证凭据相关操作（注册、密码、锁定、删除）归属 auth 域。
 *
 * @author sunshixiong
 */
@Tag(name = "账号管理", description = "账号认证凭据相关操作（注册、密码、锁定、删除）")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountApplicationService accountApplicationService;
    private final com.eagle.auth.core.infrastructure.security.BlacklistChecker blacklistChecker;

    @Operation(summary = "用户自主注册")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("permitAll()")
    public Map<String, Long> register(@Valid @RequestBody RegisterAccountRequest request) {
        blacklistChecker.checkRegister(request.getPhone(), request.getEmail(),
                com.eagle.auth.core.infrastructure.security.ClientIpHolder.get());
        Long accountId = accountApplicationService.register(
                request.getUsername(), request.getPassword(), request.getPhone(),
                request.getEmail(), request.getNickname());
        return Map.of("accountId", accountId);
    }

    @Operation(summary = "管理员创建账号")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long accountId = accountApplicationService.createAccount(
                request.getUsername(), request.getPassword(), request.getPhone(),
                request.getNickname(), request.getName(), request.getEmail());
        return Map.of("accountId", accountId);
    }

    @Operation(summary = "短信验证码重置密码")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("permitAll()")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountApplicationService.resetPasswordByPhone(
                request.getPhone(), request.getCode(), request.getNewPassword());
    }

    @Operation(summary = "绑定手机号")
    @PostMapping("/{accountId}/phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("#accountId == authentication.principal.id")
    public void bindPhone(@Parameter(description = "账号ID") @PathVariable Long accountId,
                          @Valid @RequestBody BindPhoneRequest request) {
        accountApplicationService.bindPhone(
                accountId, request.getPhone(), request.getCode());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/{accountId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void changePassword(@Parameter(description = "账号ID") @PathVariable Long accountId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        accountApplicationService.changePassword(accountId, request.getNewPassword());
    }

    @Operation(summary = "冻结账号")
    @PatchMapping("/{accountId}/freeze")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void freezeAccount(@Parameter(description = "账号ID") @PathVariable Long accountId,
                              @Valid @RequestBody FreezeAccountRequest request,
                              @AuthenticationPrincipal EagleUser principal) {
        accountApplicationService.freezeAccount(accountId,
                new FreezeAccountCommand(request.getReason(), request.getFreezeUntil(),
                        request.getRemark(),
                        principal != null ? principal.getId() : null,
                        principal != null ? principal.getName() : "admin"));
    }

    @Operation(summary = "解冻账号")
    @PatchMapping("/{accountId}/unfreeze")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void unfreezeAccount(@Parameter(description = "账号ID") @PathVariable Long accountId,
                                @AuthenticationPrincipal EagleUser principal) {
        accountApplicationService.unfreezeAccount(accountId,
                principal != null ? principal.getId() : null,
                principal != null ? principal.getName() : "admin");
    }

    @Operation(summary = "删除账号")
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deleteAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountApplicationService.deleteAccount(accountId);
    }
}

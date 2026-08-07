package com.eagle.auth.core.interfaces.controller;

import com.eagle.common.dto.EagleUser;
import com.eagle.auth.core.application.command.FreezeAccountCommand;
import com.eagle.auth.core.application.service.AccountDeletionApplicationService;
import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.interfaces.dto.request.BindPhoneRequest;
import com.eagle.auth.core.interfaces.dto.request.ChangePasswordRequest;
import com.eagle.auth.core.interfaces.dto.request.ChangePhoneRequest;
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
    private final AccountDeletionApplicationService accountDeletionApplicationService;

    @Operation(summary = "用户自主注册")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("permitAll()")
    public Map<String, Long> register(@Valid @RequestBody RegisterAccountRequest request) {
        Long accountId = accountApplicationService.register(
                request.username(), request.password(), request.phone(),
                request.email(), request.nickname());
        return Map.of("accountId", accountId);
    }

    @Operation(summary = "管理员创建账号")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long accountId = accountApplicationService.createAccount(
                request.username(), request.password(), request.phone(),
                request.nickname(), request.name(), request.email());
        return Map.of("accountId", accountId);
    }

    @Operation(summary = "短信验证码重置密码")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("permitAll()")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountApplicationService.resetPasswordByPhone(
                request.phone(), request.code(), request.newPassword());
    }

    @Operation(summary = "绑定手机号",
            description = "手机号已属其他主账号且当前为影子账号时自动归并，"
                    + "响应 merged=true，客户端应引导重新登录（当前 token 已失效）")
    @PostMapping("/{accountId}/phone")
    @PreAuthorize("#accountId == authentication.principal.id")
    public Map<String, Boolean> bindPhone(
            @Parameter(description = "账号ID") @PathVariable Long accountId,
            @Valid @RequestBody BindPhoneRequest request) {
        return Map.of("merged", accountApplicationService.bindPhone(
                accountId, request.phone(), request.code()).merged());
    }

    @Operation(summary = "修改手机号")
    @PutMapping("/{accountId}/phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("#accountId == authentication.principal.id")
    public void changePhone(@Parameter(description = "账号ID") @PathVariable Long accountId,
                            @Valid @RequestBody ChangePhoneRequest request) {
        accountApplicationService.changePhone(accountId, request.phone(), request.code());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/{accountId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("#accountId == authentication.principal.id")
    public void changePassword(@Parameter(description = "账号ID") @PathVariable Long accountId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        accountApplicationService.changePassword(accountId, request.newPassword());
    }

    @Operation(summary = "冻结账号")
    @PatchMapping("/{accountId}/freeze")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void freezeAccount(@Parameter(description = "账号ID") @PathVariable Long accountId,
                              @Valid @RequestBody FreezeAccountRequest request,
                              @AuthenticationPrincipal EagleUser principal) {
        accountApplicationService.freezeAccount(accountId,
                new FreezeAccountCommand(request.reason(), request.freezeUntil(),
                        request.remark(),
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
    @PreAuthorize("hasRole('admin') or #accountId == authentication.principal.id")
    public void deleteAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountDeletionApplicationService.deleteAccount(accountId);
    }
}

package com.eagle.system.auth.web.controller;

import com.eagle.system.auth.application.service.AccountApplicationService;
import com.eagle.system.auth.web.dto.request.BindPhoneRequest;
import com.eagle.system.auth.web.dto.request.ChangePasswordRequest;
import com.eagle.system.auth.web.dto.request.CreateAccountRequest;
import com.eagle.system.auth.web.dto.request.RegisterAccountRequest;
import com.eagle.system.auth.web.dto.request.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 用户档案、角色、部门等组织信息归属 system 域的 UserController。
 *
 * @author sunshixiong
 */
@Tag(name = "账号管理", description = "账号认证凭据相关操作（注册、密码、锁定、删除）")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountApplicationService accountApplicationService;

    /**
     * 用户自主注册
     * <p>
     * 创建 Account 后发布 AccountRegisteredEvent，system 域异步创建 User。
     */
    @Operation(summary = "用户自主注册", description = "创建账号后发布 AccountRegisteredEvent，system 域异步创建用户")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("permitAll()")
    public Map<String, Long> register(@Valid @RequestBody RegisterAccountRequest request) {
        Long accountId = accountApplicationService.register(
                request.getUsername(), request.getPassword(), request.getPhone(),
                request.getEmail(), request.getNickname());
        return Map.of("accountId", accountId);
    }

    /**
     * 管理员创建账号（含 profileHints）
     * <p>
     * profileHints（部门、角色、邮箱）通过事件传递给 system 域。
     */
    @Operation(summary = "管理员创建账号", description = "包含 profileHints（部门、角色、邮箱），通过事件传递给 system 域")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public Map<String, Long> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        Long accountId = accountApplicationService.createAccount(
                request.getUsername(), request.getPassword(), request.getPhone(),
                request.getNickname(), request.getName(),
                request.getEmail(), request.getDeptId(), request.getRoleIds());
        return Map.of("accountId", accountId);
    }

    /**
     * 短信验证码重置密码（忘记密码，未认证）
     */
    @Operation(summary = "短信验证码重置密码", description = "忘记密码场景，未认证状态下通过手机验证码重置")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("permitAll()")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountApplicationService.resetPasswordByPhone(
                request.getPhone(), request.getCode(), request.getNewPassword());
    }

    /**
     * 绑定手机号（微信登录后，已认证）
     */
    @Operation(summary = "绑定手机号", description = "微信登录后绑定手机号")
    @PostMapping("/{accountId}/phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("#accountId == authentication.principal.id")
    public void bindPhone(@Parameter(description = "账号ID") @PathVariable Long accountId,
                          @Valid @RequestBody BindPhoneRequest request) {
        accountApplicationService.bindPhone(
                accountId, request.getPhone(), request.getCode());
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "管理员可修改任意账号密码，用户只能修改自己的密码")
    @PutMapping("/{accountId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin') or #accountId == authentication.principal.id")
    public void changePassword(@Parameter(description = "账号ID") @PathVariable Long accountId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        accountApplicationService.changePassword(accountId, request.getNewPassword());
    }

    /**
     * 锁定账号
     */
    @Operation(summary = "锁定账号", description = "管理员锁定指定账号")
    @PatchMapping("/{accountId}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void lockAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountApplicationService.lockAccount(accountId);
    }

    /**
     * 解锁账号
     */
    @Operation(summary = "解锁账号", description = "管理员解锁指定账号")
    @PatchMapping("/{accountId}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void unlockAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountApplicationService.unlockAccount(accountId);
    }

    /**
     * 删除账号
     * <p>
     * 发布 AccountDeletedEvent，system 域异步级联删除 User。
     */
    @Operation(summary = "删除账号", description = "发布 AccountDeletedEvent，system 域异步级联删除用户")
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deleteAccount(@Parameter(description = "账号ID") @PathVariable Long accountId) {
        accountApplicationService.deleteAccount(accountId);
    }
}

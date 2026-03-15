package com.eagle.system.base.interfaces.rest;

import com.eagle.system.base.application.service.UserService;
import com.eagle.system.base.interfaces.dto.request.ChangePasswordRequest;
import com.eagle.system.base.interfaces.dto.request.CreateUserRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 RESTful API（轻量级 DDD）
 * <p>
 * 改进点：
 * <ul>
 *   <li>统一注入 UserService，不再分离 Command 和 Query</li>
 *   <li>删除 Command 层，直接使用 Request</li>
 *   <li>使用 UserMapper 进行对象转换</li>
 * </ul>
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户资源的 CRUD 操作")
public class UserRestController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建用户", description = "创建新用户并返回用户信息")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "用户创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "409", description = "用户名/手机号/邮箱已存在")
    })
    public Long createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户基本信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public void updateUser(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        userService.updateUser(id, request);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "修改密码", description = "修改用户密码")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "密码修改成功"),
            @ApiResponse(responseCode = "400", description = "旧密码错误或新密码不符合要求"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public void changePassword(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
    }

    /**
     * 锁定用户
     */
    @PatchMapping("/{id}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "锁定用户", description = "锁定指定用户账号")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "锁定成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "409", description = "用户已被锁定")
    })
    public void lockUser(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long id) {
        userService.lockUser(id);
    }

    /**
     * 解锁用户
     */
    @PatchMapping("/{id}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "解锁用户", description = "解锁指定用户账号")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "解锁成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "409", description = "用户未被锁定")
    })
    public void unlockUser(
            @Parameter(description = "用户ID", example = "1") @PathVariable Long id) {
        userService.unlockUser(id);
    }
}

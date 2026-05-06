package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.UserApplicationService;
import com.eagle.system.base.web.dto.request.UpdateUserRequest;
import com.eagle.system.base.web.dto.request.UserQueryRequest;
import com.eagle.system.base.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 *
 * @author sunshixiong
 */
@Tag(name = "用户管理", description = "用户档案信息管理")
@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserApplicationService userApplicationService;

    @Operation(summary = "更新用户档案")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateUser(@Parameter(description = "用户ID") @PathVariable Long id,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userApplicationService.updateUser(id, request);
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUserById(id);
    }

    @Operation(summary = "查询用户列表")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<UserResponse> queryUsers(@ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return userApplicationService.queryUsers(pageable);
    }

    @Operation(summary = "条件查询用户")
    @GetMapping("/query")
    @PreAuthorize("isAuthenticated()")
    public Page<UserResponse> queryUsers(UserQueryRequest request,
                                         @ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return userApplicationService.queryUsers(request, pageable);
    }
}

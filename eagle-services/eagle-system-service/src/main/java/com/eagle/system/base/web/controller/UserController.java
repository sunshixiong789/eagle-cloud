package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.UserApplicationService;
import com.eagle.system.base.web.dto.request.AssignDeptRequest;
import com.eagle.system.base.web.dto.request.AssignPostsRequest;
import com.eagle.system.base.web.dto.request.AssignRolesRequest;
import com.eagle.system.base.web.dto.request.UpdateUserRequest;
import com.eagle.system.base.web.dto.request.UserQueryRequest;
import com.eagle.system.base.web.dto.response.AssignedDeptResponse;
import com.eagle.system.base.web.dto.response.AssignedPostResponse;
import com.eagle.system.base.web.dto.response.AssignedRoleResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理控制器
 * <p>
 * 管理用户档案、角色分配、部门分配、岗位分配等组织信息。
 * 认证凭据操作（注册、密码、锁定、删除）由 auth 域的 AccountController 处理。
 *
 * @author sunshixiong
 */
@Tag(name = "用户管理", description = "用户档案、角色分配、部门分配、岗位分配等组织信息管理")
@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserApplicationService userApplicationService;

    /**
     * 更新用户档案信息
     */
    @Operation(summary = "更新用户档案", description = "更新用户基本信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public UserResponse updateUser(@Parameter(description = "用户ID") @PathVariable Long id,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userApplicationService.updateUser(id, request);
    }

    /**
     * 根据 ID 查询用户详情
     */
    @Operation(summary = "查询用户详情", description = "根据 ID 获取用户详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUserById(id);
    }

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "查询用户列表", description = "分页查询所有用户")
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Page<UserResponse> queryUsers(@ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return userApplicationService.queryUsers(pageable);
    }

    /**
     * 条件查询用户
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 用户分页响应
     */
    @Operation(summary = "条件查询用户", description = "根据条件分页查询用户")
    @GetMapping("/query")
    @PreAuthorize("hasRole('admin')")
    public Page<UserResponse> queryUsers(UserQueryRequest request,
                                         @ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return userApplicationService.queryUsers(request, pageable);
    }

    /**
     * 分配角色
     *
     * @param id      用户 ID
     * @param request 分配角色请求
     */
    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PatchMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void assignRoles(@Parameter(description = "用户ID") @PathVariable Long id,
                            @Valid @RequestBody AssignRolesRequest request) {
        userApplicationService.assignRoles(id, request.getRoleIds());
    }

    /**
     * 分配部门
     *
     * @param id      用户 ID
     * @param request 分配部门请求
     */
    @Operation(summary = "分配部门", description = "为用户分配部门")
    @PatchMapping("/{id}/dept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void assignDept(@Parameter(description = "用户ID") @PathVariable Long id,
                           @Valid @RequestBody AssignDeptRequest request) {
        userApplicationService.assignDept(id, request.getDeptId());
    }

    /**
     * 分配岗位
     *
     * @param id      用户 ID
     * @param request 分配岗位请求
     */
    @Operation(summary = "分配岗位", description = "为用户分配岗位")
    @PatchMapping("/{id}/posts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void assignPosts(@Parameter(description = "用户ID") @PathVariable Long id,
                            @Valid @RequestBody AssignPostsRequest request) {
        userApplicationService.assignPosts(id, request.getPostIds());
    }

    /**
     * 获取用户已分配角色列表
     *
     * @param id 用户 ID
     * @return 已分配角色列表
     */
    @Operation(summary = "获取用户已分配角色")
    @GetMapping("/{id}/roles")
    @PreAuthorize("isAuthenticated()")
    public List<AssignedRoleResponse> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUserRoles(id);
    }

    /**
     * 获取用户已分配岗位列表
     *
     * @param id 用户 ID
     * @return 已分配岗位列表
     */
    @Operation(summary = "获取用户已分配岗位")
    @GetMapping("/{id}/posts")
    @PreAuthorize("isAuthenticated()")
    public List<AssignedPostResponse> getUserPosts(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUserPosts(id);
    }

    /**
     * 获取用户所属部门
     *
     * @param id 用户 ID
     * @return 所属部门，未分配时返回 204 No Content
     */
    @Operation(summary = "获取用户所属部门")
    @GetMapping("/{id}/dept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssignedDeptResponse> getUserDept(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userApplicationService.getUserDept(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}

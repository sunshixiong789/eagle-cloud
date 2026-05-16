package com.eagle.system.base.interfaces.controller;

import com.eagle.system.base.application.service.RoleApplicationService;
import com.eagle.system.base.interfaces.dto.request.CreateRoleRequest;
import com.eagle.system.base.interfaces.dto.request.RoleQueryRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateRoleRequest;
import com.eagle.system.base.interfaces.dto.response.RoleResponse;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理控制器
 *
 * @author sunshixiong
 */
@Tag(name = "角色管理", description = "角色的增删改查及状态管理")
@RestController
@RequestMapping("roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    @Operation(summary = "创建角色", description = "创建新的角色")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleApplicationService.createRole(request);
    }

    @Operation(summary = "更新角色", description = "更新指定角色信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public RoleResponse updateRole(@Parameter(description = "角色ID") @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleApplicationService.updateRole(id, request);
    }

    @Operation(summary = "删除角色", description = "删除指定角色")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        roleApplicationService.deleteRole(id);
    }

    @Operation(summary = "查询角色详情", description = "根据 ID 获取角色详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public RoleResponse getRoleById(@Parameter(description = "角色ID") @PathVariable Long id) {
        return roleApplicationService.getRoleById(id);
    }

    @Operation(summary = "查询角色列表", description = "分页查询所有角色")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<RoleResponse> listRoles(@ParameterObject
                                        @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                        @PageableDefault Pageable pageable) {
        return roleApplicationService.listRoles(pageable);
    }

    @Operation(summary = "条件查询角色", description = "按名称、标识、状态条件查询角色")
    @GetMapping("/query")
    @PreAuthorize("hasRole('admin')")
    public Page<RoleResponse> queryRoles(RoleQueryRequest request,
                                         @ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return roleApplicationService.queryRoles(request, pageable);
    }

    @Operation(summary = "查询拥有该角色的用户列表", description = "分页查询拥有指定角色的用户")
    @GetMapping("/{id}/users")
    @PreAuthorize("hasRole('admin')")
    public Page<UserResponse> getUsersByRole(@Parameter(description = "角色ID") @PathVariable Long id,
                                             @ParameterObject
                                             @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                             @PageableDefault Pageable pageable) {
        return roleApplicationService.getUsersByRoleId(id, pageable);
    }

    @Operation(summary = "启用角色", description = "启用指定的角色")
    @PatchMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void enableRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        roleApplicationService.enableRole(id);
    }

    @Operation(summary = "禁用角色", description = "禁用指定的角色")
    @PatchMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void disableRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        roleApplicationService.disableRole(id);
    }

}

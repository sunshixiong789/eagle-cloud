package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.DeptApplicationService;
import com.eagle.system.base.web.dto.request.CreateDeptRequest;
import com.eagle.system.base.web.dto.request.UpdateDeptRequest;
import com.eagle.system.base.web.dto.response.DeptResponse;
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

import java.util.List;

/**
 * 部门管理控制器
 *
 * @author sunshixiong
 */
@Tag(name = "部门管理", description = "部门的增删改查及树形结构查询")
@RestController
@RequestMapping("depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptApplicationService deptApplicationService;

    @Operation(summary = "创建部门", description = "创建新的部门")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public DeptResponse createDept(@Valid @RequestBody CreateDeptRequest request) {
        return deptApplicationService.createDept(request);
    }

    @Operation(summary = "更新部门", description = "更新指定部门信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public DeptResponse updateDept(@Parameter(description = "部门ID") @PathVariable Long id,
                                   @Valid @RequestBody UpdateDeptRequest request) {
        return deptApplicationService.updateDept(id, request);
    }

    @Operation(summary = "删除部门", description = "删除指定部门")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deleteDept(@Parameter(description = "部门ID") @PathVariable Long id) {
        deptApplicationService.deleteDept(id);
    }

    @Operation(summary = "查询部门详情", description = "根据 ID 获取部门详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public DeptResponse getDeptById(@Parameter(description = "部门ID") @PathVariable Long id) {
        return deptApplicationService.getDeptById(id);
    }

    @Operation(summary = "查询部门列表", description = "分页查询所有部门")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<DeptResponse> queryDept(@ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return deptApplicationService.queryDept(pageable);
    }

    @Operation(summary = "查询部门树", description = "获取完整的部门树形结构")
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public List<DeptResponse> queryDeptTree() {
        return deptApplicationService.queryDeptTree();
    }

    @Operation(summary = "启用部门", description = "启用指定的部门")
    @PatchMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void enableDept(@Parameter(description = "部门ID") @PathVariable Long id) {
        deptApplicationService.enableDept(id);
    }

    @Operation(summary = "禁用部门", description = "禁用指定的部门")
    @PatchMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void disableDept(@Parameter(description = "部门ID") @PathVariable Long id) {
        deptApplicationService.disableDept(id);
    }
}

package com.eagle.audit.interfaces.controller;

import com.eagle.audit.application.AuditLogApplicationService;
import com.eagle.audit.interfaces.dto.AuditLogQueryRequest;
import com.eagle.audit.interfaces.dto.AuditLogResponse;
import com.eagle.common.exception.codes.DataErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询接口。
 *
 * <p>消费方启用 {@code eagle.audit-log.controller-enabled=true} 后自动注册。
 * 权限由 {@code eagle.audit-log.permit-role}(默认 admin)控制。
 *
 * <p>路径前缀 {@code /audit-logs}。
 *
 * @author eagle
 */
@Tag(name = "审计日志")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogApplicationService applicationService;

    @Operation(summary = "分页查询审计日志")
    @PreAuthorize("hasRole(@auditLogProperties.permitRole)")
    @GetMapping
    public Page<AuditLogResponse> query(AuditLogQueryRequest request,
                                        @ParameterObject
                                        @Parameter(description = "分页参数(page=页码从0开始, size=每页条数, sort=排序字段)")
                                        @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
                                        Pageable pageable) {
        return applicationService.query(request, pageable);
    }

    @Operation(summary = "查询审计日志详情")
    @PreAuthorize("hasRole(@auditLogProperties.permitRole)")
    @GetMapping("/{id}")
    public AuditLogResponse getById(@PathVariable Long id) {
        return applicationService.getById(id)
                .orElseThrow(DataErrorCode.DATA_NOT_FOUND::toNotFoundException);
    }
}

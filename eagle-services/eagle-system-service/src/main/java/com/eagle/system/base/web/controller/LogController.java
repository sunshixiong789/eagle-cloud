package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.LogApplicationService;
import com.eagle.system.base.web.dto.request.LogQueryRequest;
import com.eagle.system.base.web.dto.response.LogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 系统日志控制器
 * <p>
 * 提供系统日志的查询接口，支持按 ID 查询、分页查询和条件查询
 *
 * @author sunshixiong
 */
@Tag(name = "系统日志", description = "系统日志查询")
@RestController
@RequestMapping("logs")
@RequiredArgsConstructor
public class LogController {

    private final LogApplicationService logApplicationService;

    /**
     * 根据 ID 查询日志详情
     *
     * @param id 日志 ID
     * @return 日志响应 DTO
     */
    @Operation(summary = "查询日志详情", description = "根据 ID 获取日志详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public LogResponse getLogById(@Parameter(description = "日志ID") @PathVariable Long id) {
        return logApplicationService.getLogById(id);
    }

    /**
     * 分页查询所有日志
     *
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    @Operation(summary = "查询日志列表", description = "分页查询所有系统日志")
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Page<LogResponse> queryLogs(Pageable pageable) {
        return logApplicationService.queryLogs(pageable);
    }

    /**
     * 条件分页查询日志
     * <p>
     * 支持按日志类型、状态、用户名、请求 URI、IP、时间范围组合查询
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    @Operation(summary = "条件查询日志", description = "根据条件分页查询系统日志")
    @GetMapping("/query")
    @PreAuthorize("hasRole('admin')")
    public Page<LogResponse> queryLogs(LogQueryRequest request, Pageable pageable) {
        return logApplicationService.queryLogs(request, pageable);
    }

    /**
     * 导出日志（xlsx/csv，最多10000条）
     *
     * @param request  查询条件
     * @param format   导出格式（xlsx 或 csv，默认 xlsx）
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    @Operation(summary = "导出日志（xlsx/csv，最多10000条）")
    @GetMapping("/export")
    @PreAuthorize("hasRole('admin')")
    public void exportLogs(LogQueryRequest request,
                           @RequestParam(defaultValue = "xlsx") String format,
                           HttpServletResponse response) throws IOException {
        logApplicationService.exportLogs(request, format, response);
    }
}

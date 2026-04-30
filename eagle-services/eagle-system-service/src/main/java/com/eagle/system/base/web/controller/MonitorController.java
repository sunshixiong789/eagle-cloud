package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.MonitorApplicationService;
import com.eagle.system.base.web.dto.request.LoginLogQueryRequest;
import com.eagle.system.base.web.dto.response.LoginLogStatsResponse;
import com.eagle.system.base.web.dto.response.OnlineUserListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统监控接口。
 * <p>
 * 服务器指标数据通过 Spring Boot Actuator 端点提供（/actuator/metrics、/actuator/health）。
 */
@Tag(name = "系统监控")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorApplicationService monitorService;

    /**
     * 获取在线用户列表。
     *
     * @return 在线用户列表响应
     */
    @Operation(summary = "获取在线用户列表")
    @GetMapping("/online-users")
    @PreAuthorize("hasRole('admin')")
    public OnlineUserListResponse listOnlineUsers() {
        return monitorService.listOnlineUsers();
    }

    /**
     * 强制下线指定用户。
     *
     * @param tokenId 目标用户 JWT JTI
     */
    @Operation(summary = "强制下线指定用户")
    @DeleteMapping("/online-users/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void forceLogout(@PathVariable String tokenId) {
        monitorService.forceLogout(tokenId);
    }

    /**
     * 查询登录日志（含今日统计）。
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 登录日志统计响应
     */
    @Operation(summary = "查询登录日志")
    @GetMapping("/login-logs")
    @PreAuthorize("hasRole('admin')")
    public LoginLogStatsResponse queryLoginLogs(LoginLogQueryRequest request, Pageable pageable) {
        return monitorService.queryLoginLogs(request, pageable);
    }
}

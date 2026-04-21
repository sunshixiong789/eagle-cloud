package com.eagle.system.upms.application.service;

import com.eagle.system.auth.domain.port.OnlineUserInfo;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import com.eagle.common.exception.codes.OperationErrorCode;
import com.eagle.system.upms.domain.model.enums.LogStatus;
import com.eagle.system.upms.domain.model.enums.LogType;
import com.eagle.system.upms.domain.repository.LogRepository;
import com.eagle.system.upms.web.dto.request.LogQueryRequest;
import com.eagle.system.upms.web.dto.request.LoginLogQueryRequest;
import com.eagle.system.upms.web.dto.response.LoginLogStatsResponse;
import com.eagle.system.upms.web.dto.response.OnlineUserListResponse;
import com.eagle.system.upms.web.dto.response.OnlineUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控应用服务：在线用户管理、登录日志统计。
 * <p>
 * 服务器监控通过 Spring Boot Actuator 端点（/actuator/**）直接提供。
 */
@Service
@RequiredArgsConstructor
public class MonitorApplicationService {

    private final OnlineUserPort onlineUserPort;
    private final LogApplicationService logApplicationService;
    private final LogRepository logRepository;

    /**
     * 获取当前在线用户列表。
     *
     * @return 在线用户列表响应
     */
    public OnlineUserListResponse listOnlineUsers() {
        List<OnlineUserInfo> infos = onlineUserPort.listOnlineUsers();
        List<OnlineUserResponse> responses = infos.stream()
                .map(info -> OnlineUserResponse.builder()
                        .tokenId(info.tokenId())
                        .userId(info.userId())
                        .username(info.username())
                        .ip(info.ip())
                        .loginTime(info.loginTime())
                        .lastActiveTime(info.lastActiveTime())
                        .browser(info.browser())
                        .os(info.os())
                        .build())
                .toList();
        return new OnlineUserListResponse(responses.size(), responses);
    }

    /**
     * 强制下线指定用户（通过 JWT JTI 识别）。
     * <p>
     * 禁止踢出当前登录用户自身。
     *
     * @param tokenId 目标用户的 JWT JTI
     */
    public void forceLogout(String tokenId) {
        // 禁止踢出自己：当前 token 与目标 tokenId 相同时拒绝
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentJti = getCurrentJti(auth);
        if (currentJti != null && currentJti.equals(tokenId)) {
            throw OperationErrorCode.OPERATION_NOT_ALLOWED.toDomainException();
        }
        onlineUserPort.forceLogout(tokenId);
    }

    /**
     * 查询登录日志，并附带今日登录统计数据。
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 登录日志统计响应
     */
    @Transactional(readOnly = true)
    public LoginLogStatsResponse queryLoginLogs(LoginLogQueryRequest request, Pageable pageable) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        LogQueryRequest logRequest = new LogQueryRequest();
        logRequest.setUsername(request.getUsername());
        logRequest.setRemoteAddr(request.getIp());
        logRequest.setStatus(request.getStatus());
        logRequest.setStartTime(request.getStartTime());
        logRequest.setEndTime(request.getEndTime());
        logRequest.setLogType(LogType.LOGIN.name());

        long todayTotal = logRepository.countByLogTypeAndPeriod(LogType.LOGIN, todayStart, tomorrowStart);
        long todayFail = logRepository.countByLogTypeAndStatusAndPeriod(
                LogType.LOGIN, LogStatus.FAILURE, todayStart, tomorrowStart);
        long todayUniqueUsers = logRepository.countDistinctUsernameByLogTypeAndPeriod(
                LogType.LOGIN, todayStart, tomorrowStart);

        return LoginLogStatsResponse.builder()
                .todayTotal(todayTotal)
                .todayFail(todayFail)
                .todayUniqueUsers(todayUniqueUsers)
                .page(logApplicationService.queryLogs(logRequest, pageable))
                .build();
    }

    /**
     * 从 Authentication 中提取 JWT JTI。
     *
     * @param auth 当前认证对象
     * @return JTI 字符串，若无法提取则返回 null
     */
    private String getCurrentJti(Authentication auth) {
        if (auth != null && auth.getCredentials() instanceof Jwt jwt) {
            return jwt.getId();
        }
        return null;
    }
}

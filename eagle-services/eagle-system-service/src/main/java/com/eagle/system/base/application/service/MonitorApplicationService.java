package com.eagle.system.base.application.service;

import com.eagle.audit.annotation.AuditLog;
import com.eagle.common.exception.codes.OperationErrorCode;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.infrastructure.remote.AuthClientFacade;
import com.eagle.system.base.infrastructure.remote.dto.OnlineUserSnapshot;
import com.eagle.system.base.domain.model.enums.LogType;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.interfaces.dto.request.LogQueryRequest;
import com.eagle.system.base.interfaces.dto.request.LoginLogQueryRequest;
import com.eagle.system.base.interfaces.dto.response.LogResponse;
import com.eagle.system.base.interfaces.dto.response.LoginLogItemResponse;
import com.eagle.system.base.interfaces.dto.response.LoginLogStatsResponse;
import com.eagle.system.base.interfaces.dto.response.OnlineUserListResponse;
import com.eagle.system.base.interfaces.dto.response.OnlineUserResponse;
import com.eagle.system.base.interfaces.dto.response.ServiceInstanceInfo;
import com.eagle.system.base.interfaces.dto.response.ServiceStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 监控应用服务：在线用户管理、登录日志统计、服务注册中心探测。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorApplicationService {

    private final AuthClientFacade authClientFacade;
    private final LogApplicationService logApplicationService;
    private final LogRepository logRepository;
    private final DiscoveryClient discoveryClient;

    /**
     * 内部 actuator 探测专用 RestClient（短超时，探测失败不影响主流程）。
     * 连接超时 2s，读取超时 3s，避免慢节点阻塞整体响应。
     */
    private final RestClient actuatorClient = RestClient.builder()
            .requestFactory(actuatorRequestFactory())
            .build();

    // -------------------------------------------------------------------------
    // 在线用户
    // -------------------------------------------------------------------------

    private static SimpleClientHttpRequestFactory actuatorRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    /**
     * 在线用户列表。auth 端按 jti 存储(同一用户多端登录会有多条),
     * 此处按用户身份去重,只保留 loginTime 最新的一条;
     * tokenId 字段保留,管理员仍可针对该会话强制下线。
     */
    public OnlineUserListResponse listOnlineUsers() {
        List<OnlineUserSnapshot> infos = authClientFacade.listOnlineUsers();
        List<OnlineUserResponse> responses = infos.stream()
                .collect(Collectors.toMap(
                        this::onlineUserDedupKey,
                        Function.identity(),
                        MonitorApplicationService::pickLatestSession,
                        LinkedHashMap::new))
                .values()
                .stream()
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
     * 按账号去重后的当前在线用户数。
     * <p>仅返回数量，不构建完整 DTO 列表，供 Dashboard 等聚合接口复用。
     * 下游 auth-service 不可达 / 熔断开路时，{@link AuthClientFacade} 已降级为空列表，
     * 此处自然得到 0，调用方无需额外处理。
     */
    public long countOnlineUsers() {
        return authClientFacade.listOnlineUsers().stream()
                .map(this::onlineUserDedupKey)
                .distinct()
                .count();
    }

    private String onlineUserDedupKey(OnlineUserSnapshot info) {
        if (info.userId() != null) {
            return "u:" + info.userId();
        }
        // userId 缺失退化到 username;username 也缺失退化到 tokenId(此时无法去重,保留原条目)
        return "n:" + (info.username() != null ? info.username() : info.tokenId());
    }

    private static OnlineUserSnapshot pickLatestSession(OnlineUserSnapshot existing,
                                                        OnlineUserSnapshot incoming) {
        LocalDateTime a = existing.loginTime();
        LocalDateTime b = incoming.loginTime();
        if (a == null) {
            return incoming;
        }
        if (b == null) {
            return existing;
        }
        return b.isAfter(a) ? incoming : existing;
    }

    // -------------------------------------------------------------------------
    // 登录日志
    // -------------------------------------------------------------------------

    @AuditLog(module = "系统监控", action = "强制下线用户")
    public void forceLogout(String tokenId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentJti = getCurrentJti(auth);
        if (currentJti != null && currentJti.equals(tokenId)) {
            throw OperationErrorCode.OPERATION_NOT_ALLOWED.toDomainException();
        }
        authClientFacade.forceLogout(tokenId);
    }

    // -------------------------------------------------------------------------
    // 服务注册中心监控
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public LoginLogStatsResponse queryLoginLogs(LoginLogQueryRequest request, Pageable pageable) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        LogQueryRequest logRequest = new LogQueryRequest();
        logRequest.setUsername(request.getUsername());
        logRequest.setRemoteAddr(request.getIp());
        logRequest.setStatus("FAIL".equals(request.getStatus()) ? "FAILURE" : request.getStatus());
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
                .page(logApplicationService.queryLogs(logRequest, pageable).map(this::toLoginLogItem))
                .build();
    }

    /**
     * 从 Nacos 拉取所有服务，并并行探测每个服务第一个健康实例的 actuator 指标。
     * <p>
     * 探测采用 CompletableFuture 并行执行，避免慢节点串行阻塞。
     * 探测失败（服务未暴露 actuator、网络超时等）时对应字段为 null，不影响其他服务。
     *
     * @return 服务状态列表（含 CPU/内存指标）
     */
    public List<ServiceStatusResponse> listServices() {
        List<CompletableFuture<ServiceStatusResponse>> futures = discoveryClient.getServices()
                .stream()
                .map(serviceId -> CompletableFuture.supplyAsync(() -> buildServiceStatus(serviceId)))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    // -------------------------------------------------------------------------
    // Actuator 探测工具方法
    // -------------------------------------------------------------------------

    private ServiceStatusResponse buildServiceStatus(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        List<ServiceInstanceInfo> infos = instances.stream()
                .map(inst -> ServiceInstanceInfo.builder()
                        .instanceId(inst.getInstanceId())
                        .host(inst.getHost())
                        .port(inst.getPort())
                        .metadata(inst.getMetadata())
                        .build())
                .toList();

        String displayName = instances.isEmpty() ? serviceId
                : instances.getFirst().getMetadata().getOrDefault("spring-doc-name", serviceId);

        // 取第一个实例的 URI 作为 actuator 探测目标
        String baseUrl = instances.isEmpty() ? null : instances.getFirst().getUri().toString();

        Double cpu = null;
        Long memUsed = null;
        Long memMax = null;
        String healthStatus = null;

        if (baseUrl != null) {
            cpu = fetchMetricValue(baseUrl, "system.cpu.usage");
            memUsed = fetchMetricLong(baseUrl, "jvm.memory.used");
            memMax = fetchMetricLong(baseUrl, "jvm.memory.max");
            healthStatus = fetchHealthStatus(baseUrl);
        }

        return ServiceStatusResponse.builder()
                .serviceId(serviceId)
                .displayName(displayName)
                .status(infos.isEmpty() ? "DOWN" : "UP")
                .healthStatus(healthStatus)
                .healthyCount(infos.size())
                .instances(infos)
                .cpuUsage(cpu)
                .memUsed(memUsed)
                .memMax(memMax)
                .build();
    }

    /**
     * 探测指定 actuator 指标的 VALUE 值（0.0~1.0 或字节数等）。
     * 任何异常（404/超时/网络不通）均返回 null。
     */
    @SuppressWarnings("unchecked")
    private Double fetchMetricValue(String baseUrl, String metricName) {
        try {
            Map<String, Object> resp = actuatorClient.get()
                    .uri(baseUrl + "/actuator/metrics/" + metricName)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) return null;
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) resp.get("measurements");
            if (measurements == null) return null;
            return measurements.stream()
                    .filter(m -> "VALUE".equals(m.get("statistic")))
                    .map(m -> ((Number) m.get("value")).doubleValue())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.debug("actuator metric probe failed [{}/{}]: {}", baseUrl, metricName, e.getMessage());
            return null;
        }
    }

    /**
     * 与 fetchMetricValue 相同，结果转为 Long（用于内存字节数）。
     */
    private Long fetchMetricLong(String baseUrl, String metricName) {
        Double val = fetchMetricValue(baseUrl, metricName);
        return val != null ? val.longValue() : null;
    }

    /**
     * 探测 actuator /health 端点，返回顶层 status 字符串。
     * 失败时返回 null。
     */
    @SuppressWarnings("unchecked")
    private String fetchHealthStatus(String baseUrl) {
        try {
            Map<String, Object> resp = actuatorClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve()
                    .body(Map.class);
            return resp != null ? (String) resp.get("status") : null;
        } catch (Exception e) {
            log.debug("actuator health probe failed [{}]: {}", baseUrl, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // 登录日志私有映射
    // -------------------------------------------------------------------------

    private LoginLogItemResponse toLoginLogItem(LogResponse log) {
        return LoginLogItemResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .ip(log.getRemoteAddr())
                .browser(parseBrowser(log.getUserAgent()))
                .os(parseOs(log.getUserAgent()))
                .status("FAILURE".equals(log.getStatus()) ? "FAIL" : log.getStatus())
                .loginTime(log.getCreateTime() != null ? log.getCreateTime().toString() : null)
                .failReason(log.getException())
                .build();
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        return "Unknown";
    }

    private String parseOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Macintosh") || ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Unknown";
    }

    private String getCurrentJti(Authentication auth) {
        if (auth != null && auth.getCredentials() instanceof Jwt jwt) {
            return jwt.getId();
        }
        return null;
    }
}

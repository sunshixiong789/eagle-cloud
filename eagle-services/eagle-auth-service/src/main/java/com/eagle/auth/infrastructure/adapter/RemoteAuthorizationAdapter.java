package com.eagle.auth.infrastructure.adapter;

import com.eagle.auth.domain.port.AuthorizationInfo;
import com.eagle.auth.domain.port.AuthorizationPort;
import com.eagle.auth.infrastructure.remote.SystemAuthorizationClient;
import com.eagle.auth.infrastructure.remote.dto.AuthorizationInfoDto;
import com.eagle.common.exception.NotFoundException;
import tools.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * {@link AuthorizationPort} 的远程实现:通过 RestClient 调 eagle-system-service
 * 的 {@code /internal/authorization/{accountId}} 内部端点,把姓名 + 角色码注入到 JWT claims。
 *
 * <p><strong>容错策略</strong> (rules/21-resilience.md):
 * <ul>
 *   <li>{@code @Retry} 对瞬时异常做指数退避重试,最多 3 次</li>
 *   <li>{@code @CircuitBreaker} 包裹整个调用:连续失败触发熔断,半开探测自动恢复</li>
 *   <li>{@code fallbackMethod} 在熔断开路 / 重试耗尽 / 下游不可达时,读取 Redis 缓存里
 *       该 accountId <em>最近一次成功</em>的授权快照(TTL 24h),保证认证流程不被下游故障击穿</li>
 *   <li>HTTP 404 ({@link NotFoundException}):意味着"账号无关联 User"——这是<strong>业务正常</strong>
 *       结果,直接返回 {@code empty()},<em>不</em>计入熔断器失败、<em>不</em>触发降级</li>
 * </ul>
 *
 * <p><strong>缓存键</strong>: {@code eagle:auth:authorization:{accountId}},值是 {@link AuthorizationInfoDto}
 * 的 JSON。每次成功调用都覆盖写一份(write-through);失败时只读不写。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteAuthorizationAdapter implements AuthorizationPort {

    private static final String CACHE_KEY_PREFIX = "eagle:auth:authorization:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final SystemAuthorizationClient systemAuthorizationClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Retry(name = "eagle-default")
    @CircuitBreaker(name = "eagle-default", fallbackMethod = "findFromCache")
    public Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId) {
        AuthorizationInfoDto dto;
        try {
            dto = systemAuthorizationClient.findByAccountId(accountId);
        } catch (NotFoundException ex) {
            // 业务正常:账号无关联 User → 返回 empty,不触发降级、不计入熔断
            log.debug("authorization not found, accountId={}", accountId);
            return Optional.empty();
        }
        cache(accountId, dto);
        return Optional.of(toAuthorizationInfo(dto));
    }

    /**
     * 降级路径:熔断开路 / 重试耗尽 / 下游不可达时,从 Redis 读最近一次成功的快照。
     *
     * <p>fallback 方法签名规约:同一原方法参数列表 + 末尾追加 {@link Throwable}。
     * 仅对"下游不可达类"异常生效,编程错误(NPE / IllegalState 等)直接上抛,不被本方法掩盖。
     *
     * <p>package-private 暴露以便单测直接验证降级行为(不必启动 Resilience4J AOP)。
     */
    @SuppressWarnings("unused")
    Optional<AuthorizationInfo> findFromCache(Long accountId, Throwable ex) {
        if (!isFallbackEligible(ex)) {
            sneakyThrow(ex);
        }
        Optional<AuthorizationInfo> cached = readCache(accountId);
        if (cached.isPresent()) {
            log.warn("authorization remote unavailable, served from cache: accountId={}, reason={}",
                    accountId, ex.toString());
            return cached;
        }
        log.warn("authorization remote unavailable, no cache: accountId={}, reason={}",
                accountId, ex.toString());
        return Optional.empty();
    }

    private static AuthorizationInfo toAuthorizationInfo(AuthorizationInfoDto dto) {
        Set<String> roleCodes = dto.roleCodes() != null ? dto.roleCodes() : Set.of();
        return new AuthorizationInfo(dto.name(), roleCodes);
    }

    private void cache(Long accountId, AuthorizationInfoDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(cacheKey(accountId), json, CACHE_TTL);
        } catch (RuntimeException e) {
            // 缓存写失败不影响主流程（含 Jackson 3 序列化异常，已是 RuntimeException）
            log.warn("authorization cache write failed: accountId={}", accountId, e);
        }
    }

    private Optional<AuthorizationInfo> readCache(Long accountId) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(accountId));
            if (json == null) {
                return Optional.empty();
            }
            AuthorizationInfoDto dto = objectMapper.readValue(json, AuthorizationInfoDto.class);
            return Optional.of(toAuthorizationInfo(dto));
        } catch (RuntimeException e) {
            log.warn("authorization cache read failed: accountId={}", accountId, e);
            return Optional.empty();
        }
    }

    private static String cacheKey(Long accountId) {
        return CACHE_KEY_PREFIX + accountId;
    }

    /**
     * 只有"下游不可达类"异常才走降级。NPE / IllegalState / IllegalArgument 等编程错误
     * 必须上抛,由全局异常处理器返回 5xx,避免掩盖 bug。
     */
    private static boolean isFallbackEligible(Throwable ex) {
        return ex instanceof RestClientException
                || ex instanceof CallNotPermittedException;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }
}

package com.eagle.system.base.infrastructure.remote;

import com.eagle.system.base.infrastructure.remote.dto.AccountBlacklistSnapshot;
import com.eagle.system.base.infrastructure.remote.dto.AccountBatchRequest;
import com.eagle.system.base.infrastructure.remote.dto.AccountSnapshot;
import com.eagle.system.base.infrastructure.remote.dto.OnlineUserSnapshot;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;

/**
 * auth-service 内部 API 调用门面 — 集中熔断 + 降级。
 *
 * <p>{@code @CircuitBreaker} 注解依赖 Spring AOP 代理,只对 <strong>public 方法 + 跨 Bean 调用</strong>
 * 生效。把对 {@link AuthOnlineUserClient} / {@link AuthAccountBlacklistClient} 的调用统一收敛在本类的
 * public 方法里,业务侧 ({@code UserApplicationService} / {@code MonitorApplicationService})
 * 通过 Spring 注入本 Bean 即可拿到熔断器保护。
 *
 * <p>降级策略:
 * <ul>
 *   <li>{@link RestClientException} / {@link CallNotPermittedException}(熔断器开路) → 降级返回业务可读默认值</li>
 *   <li>编程错误(NPE / IllegalState 等) → 通过 starter 默认 {@code ignoreExceptions} 不计入熔断,
 *       直接上抛由全局异常处理器返回 5xx,不掩盖 bug</li>
 * </ul>
 *
 * <p>对应 review LOW-3:之前 {@code isOnline} / {@code enrichBlacklistStatus} 是 private 方法,
 * AOP 不生效,只有 {@code MonitorApplicationService.listOnlineUsers} 一处真正受熔断器保护。
 * 本门面把保护面拉齐到所有跨服务读链路。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClientFacade {

    private final AuthOnlineUserClient onlineUserClient;
    private final AuthAccountBlacklistClient blacklistClient;
    private final AuthAccountClient accountClient;

    /**
     * 列出 auth-service 维护的全部在线用户。
     * 下游不可达 / 熔断开路 / 反序列化失败 → 降级返回空列表。
     */
    @CircuitBreaker(name = "eagle-default", fallbackMethod = "listOnlineUsersFallback")
    public List<OnlineUserSnapshot> listOnlineUsers() {
        return onlineUserClient.listOnlineUsers();
    }

    @SuppressWarnings("unused")
    private List<OnlineUserSnapshot> listOnlineUsersFallback(Throwable ex) {
        if (!isFallbackEligible(ex)) {
            sneakyThrow(ex);
        }
        log.warn("listOnlineUsers 降级为空列表(可能熔断开路或下游故障)", ex);
        return List.of();
    }

    /**
     * 反查某账号当前在线 JTI 列表。
     * 下游不可达 / 熔断开路 → 降级返回空列表 (= 视为离线)。
     */
    @CircuitBreaker(name = "eagle-default", fallbackMethod = "listJtisByAccountFallback")
    public List<String> listJtisByAccount(Long accountId) {
        return onlineUserClient.listJtisByAccount(accountId);
    }

    @SuppressWarnings("unused")
    private List<String> listJtisByAccountFallback(Long accountId, Throwable ex) {
        if (!isFallbackEligible(ex)) {
            sneakyThrow(ex);
        }
        log.warn("listJtisByAccount 降级为空列表: accountId={}", accountId, ex);
        return List.of();
    }

    /**
     * 查询某账号当前生效的黑名单记录。
     * 下游不可达 / 熔断开路 → 降级返回 204 No Content (= 视为非黑名单)。
     */
    @CircuitBreaker(name = "eagle-default", fallbackMethod = "findBlacklistByAccountIdFallback")
    public ResponseEntity<AccountBlacklistSnapshot> findBlacklistByAccountId(Long accountId) {
        return blacklistClient.findByAccountId(accountId);
    }

    @SuppressWarnings("unused")
    private ResponseEntity<AccountBlacklistSnapshot> findBlacklistByAccountIdFallback(
            Long accountId, Throwable ex) {
        if (!isFallbackEligible(ex)) {
            sneakyThrow(ex);
        }
        log.warn("findBlacklistByAccountId 降级为非黑名单: accountId={}", accountId, ex);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 强制下线指定 token。<strong>不</strong>走熔断/降级 — 这是写操作,必须由调用方感知失败。
     */
    public void forceLogout(String tokenId) {
        onlineUserClient.forceLogout(tokenId);
    }

    /**
     * 全量账号数(权威源)。下游不可达 / 熔断开路 → 降级返回 -1,调用方据此选择是否走本地兜底。
     * <p>选 -1 而非 0,是为了让调用方能区分"真的没账号"和"远程不可用"两种语义。
     */
    @CircuitBreaker(name = "eagle-default", fallbackMethod = "countAccountsFallback")
    public long countAccounts() {
        return accountClient.count();
    }

    /**
     * 批量查询账号快照。仅用于列表展示增强，下游不可用时降级为空，不阻断 system 用户列表。
     */
    @CircuitBreaker(name = "eagle-default", fallbackMethod = "findAccountsFallback")
    public List<AccountSnapshot> findAccounts(Set<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        return accountClient.findBatch(new AccountBatchRequest(accountIds));
    }

    @SuppressWarnings("unused")
    private List<AccountSnapshot> findAccountsFallback(Set<Long> accountIds, Throwable ex) {
        if (!isFallbackEligible(ex)) {
            sneakyThrow(ex);
        }
        log.warn("findAccounts 降级为空列表: accountCount={}", accountIds.size(), ex);
        return List.of();
    }

    @SuppressWarnings("unused")
    private long countAccountsFallback(Throwable ex) {
        if (!isFallbackEligible(ex)) {
            sneakyThrow(ex);
        }
        log.warn("countAccounts 降级为 -1(可能熔断开路或下游故障)", ex);
        return -1L;
    }

    /**
     * 判断异常是否符合降级条件:只有"下游不可达"类异常才降级,编程错误必须上抛。
     */
    private boolean isFallbackEligible(Throwable ex) {
        return ex instanceof RestClientException
                || ex instanceof CallNotPermittedException;
    }

    /**
     * 把 Throwable 重新抛出,绕过 checked exception 限制。
     * 仅在 fallback 内对不合适降级的异常使用。
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }
}

package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.BlacklistAddedEvent;
import com.eagle.system.auth.domain.event.BlacklistRemovedEvent;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import com.eagle.system.auth.domain.repository.AccountRepository;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 黑名单变更事件处理：同步 Redis 缓存 + 强制下线受影响账号。
 *
 * <p>加入黑名单时，除把 ({@code type}, {@code value}) 写入 {@link BlacklistCacheStore} 外，
 * 还会解析出受影响的 accountId，对其当前所有在线 JTI 调用
 * {@link OnlineUserPort#forceLogout(String)}，写入 token 黑名单，让
 * {@link com.eagle.system.auth.infrastructure.security.BlacklistAwareJwtDecoder}
 * 在下一次 JWT 校验时立即拒绝。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistCacheSyncHandler {

    private final BlacklistCacheStore cacheStore;
    private final OnlineUserPort onlineUserPort;
    private final AccountRepository accountRepository;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onAdded(BlacklistAddedEvent event) {
        cacheStore.add(event.type(), event.value());
        log.info("blacklist cache add: type={}, value={}", event.type(), event.value());
        forceLogoutAffected(event.type(), event.value());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemoved(BlacklistRemovedEvent event) {
        cacheStore.remove(event.type(), event.value());
        log.info("blacklist cache remove: type={}, value={}", event.type(), event.value());
    }

    /**
     * 根据黑名单类型解析出受影响的 accountId，并强制下线其全部在线会话。
     *
     * <p>IP / EMAIL 类型无法直接映射到单一账号，由 JWT 解码侧 + 后续登录拦截兜底，
     * 此处不做扫描型下线（避免 Redis SCAN 大锁）。
     */
    private void forceLogoutAffected(BlacklistType type, String value) {
        Long accountId = resolveAccountId(type, value);
        if (accountId == null) {
            return;
        }
        List<String> jtis = onlineUserPort.listJtisByAccount(accountId);
        if (jtis.isEmpty()) {
            return;
        }
        jtis.forEach(onlineUserPort::forceLogout);
        log.info("blacklist forced logout: accountId={}, sessionCount={}, type={}, value={}",
                accountId, jtis.size(), type, value);
    }

    private Long resolveAccountId(BlacklistType type, String value) {
        return switch (type) {
            case ACCOUNT_ID -> parseLong(value);
            case PHONE -> accountRepository.findByPhone(value).map(Account::getId).orElse(null);
            case OPENID -> accountRepository.findByWechatBindingOpenid(value)
                    .or(() -> accountRepository.findByWechatBindingUnionid(value))
                    .or(() -> accountRepository.findByWechatBindingWebOpenid(value))
                    .or(() -> accountRepository.findByWechatBindingMpOpenid(value))
                    .map(Account::getId).orElse(null);
            case EMAIL, IP -> null;
        };
    }

    private static Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

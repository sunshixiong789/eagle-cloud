package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.BlacklistAddedEvent;
import com.eagle.system.auth.domain.event.BlacklistRemovedEvent;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 黑名单变更事件同步 Redis 缓存
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistCacheSyncHandler {

    private final BlacklistCacheStore cacheStore;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdded(BlacklistAddedEvent event) {
        cacheStore.add(event.type(), event.value());
        log.info("blacklist cache add: type={}, value={}", event.type(), event.value());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemoved(BlacklistRemovedEvent event) {
        cacheStore.remove(event.type(), event.value());
        log.info("blacklist cache remove: type={}, value={}", event.type(), event.value());
    }
}

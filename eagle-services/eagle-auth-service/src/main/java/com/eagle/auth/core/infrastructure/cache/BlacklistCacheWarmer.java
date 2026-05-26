package com.eagle.auth.core.infrastructure.cache;

import com.eagle.auth.core.domain.model.Blacklist;
import com.eagle.auth.core.domain.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动期黑名单全量加载至 Redis
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistCacheWarmer {

    private final BlacklistRepository repository;
    private final BlacklistCacheStore cacheStore;

    @Value("${eagle.auth.blacklist.cache-warm-on-startup:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!enabled) {
            log.info("blacklist cache warmer disabled");
            return;
        }
        long start = System.currentTimeMillis();
        List<Blacklist> all = repository.findAllActiveForCacheWarmup(LocalDateTime.now());
        for (Blacklist b : all) {
            cacheStore.add(b.getType(), b.getValue());
        }
        log.info("blacklist cache warmed: count={}, costMs={}",
                all.size(), System.currentTimeMillis() - start);
    }
}

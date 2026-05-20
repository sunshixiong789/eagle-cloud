package com.eagle.system.auth.application.service;

import com.eagle.system.auth.application.command.AddBlacklistCommand;
import com.eagle.system.auth.application.command.BlacklistQuery;
import com.eagle.system.auth.application.mapper.BlacklistMapper;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.system.auth.domain.repository.BlacklistRepository;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import com.eagle.system.auth.interfaces.dto.response.BlacklistResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 黑名单应用服务（全局，不区分租户）
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistApplicationService {

    private final BlacklistRepository repository;
    private final BlacklistMapper mapper;
    private final BlacklistCacheStore cacheStore;

    @Transactional(readOnly = true)
    public Page<BlacklistResponse> queryBlacklist(BlacklistQuery query, Pageable pageable) {
        Page<Blacklist> page;
        if (query.type() != null && query.value() != null && !query.value().isBlank()) {
            page = repository.findByTypeAndValueContaining(query.type(), query.value(), pageable);
        } else if (query.type() != null) {
            page = repository.findByType(query.type(), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(mapper::toResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public BlacklistResponse addToBlacklist(AddBlacklistCommand cmd) {
        if (cmd.expiresAt() != null && !cmd.expiresAt().isAfter(LocalDateTime.now())) {
            throw AuthErrorCode.ACCOUNT_FREEZE_UNTIL_INVALID.toDomainException();
        }
        repository.findByTypeAndValue(cmd.type(), cmd.value()).ifPresent(b -> {
            throw AuthErrorCode.BLACKLIST_DUPLICATE.toConflictException();
        });
        Blacklist blacklist = Blacklist.create(
                cmd.type(), cmd.value(), cmd.reason(), cmd.expiresAt(),
                cmd.operatorId(), cmd.operatorName());
        Blacklist saved = repository.save(blacklist);
        log.info("blacklist added: id={}, type={}, value={}",
                saved.getId(), saved.getType(), saved.getValue());
        return mapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFromBlacklist(Long id) {
        Blacklist blacklist = repository.findById(id)
                .orElseThrow(AuthErrorCode.BLACKLIST_NOT_FOUND::toNotFoundException);
        blacklist.publishRemovedEvent();
        repository.save(blacklist);
        repository.deleteById(id);
        log.info("blacklist removed: id={}, type={}, value={}",
                id, blacklist.getType(), blacklist.getValue());
    }

    /**
     * 检查指定身份是否在黑名单中（含懒过期判断）。
     *
     * <p>纯只读：缓存命中但 DB 行已不存在 / 已过期时，仅从 Redis 移除缓存条目，
     * 不再调用同类 {@link #removeFromBlacklist}（同类内部调用会绕过 AOP 代理导致事务失效）。
     * DB 中的过期记录由独立定时任务 / 管理端显式 remove 清理。
     *
     * @param type  黑名单类型
     * @param value 黑名单值
     * @return {@code true} 表示仍在黑名单且未过期
     */
    public boolean isBlacklisted(BlacklistType type, String value) {
        if (!cacheStore.isMember(type, value)) {
            return false;
        }
        Optional<Blacklist> entry = repository.findByTypeAndValue(type, value);
        if (entry.isEmpty()) {
            cacheStore.remove(type, value);
            return false;
        }
        if (entry.get().isExpired(LocalDateTime.now())) {
            // 只清缓存，避免同类内部调用 removeFromBlacklist 失去事务；
            // DB 行由后台清理任务在事务上下文中删除。
            cacheStore.remove(type, value);
            log.debug("blacklist entry expired (cache evicted only), id={}", entry.get().getId());
            return false;
        }
        return true;
    }

    /**
     * 清理过期的黑名单条目（独立事务，供定时任务调用）。
     *
     * @return 被清理的条目数
     */
    @Transactional(rollbackFor = Exception.class)
    public int purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        Page<Blacklist> all = repository.findAll(org.springframework.data.domain.Pageable.unpaged());
        int removed = 0;
        for (Blacklist entry : all.getContent()) {
            if (entry.isExpired(now)) {
                entry.publishRemovedEvent();
                repository.delete(entry);
                removed++;
            }
        }
        if (removed > 0) {
            log.info("purged expired blacklist entries: count={}", removed);
        }
        return removed;
    }
}

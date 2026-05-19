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
import com.eagle.tenant.TenantContextHolder;
import com.eagle.tenant.annotation.TenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 黑名单应用服务
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

    @TenantFilter
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

    @TenantFilter
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

    @TenantFilter
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
     * 检查指定身份是否在黑名单中（含懒过期逻辑）。
     *
     * @param type  黑名单类型
     * @param value 黑名单值
     * @return {@code true} 表示仍在黑名单且未过期
     */
    @TenantFilter
    public boolean isBlacklisted(BlacklistType type, String value) {
        String tenantId = TenantContextHolder.getTenantId();
        if (cacheStore.isMember(tenantId, type, value)) {
            Optional<Blacklist> entry = repository.findByTypeAndValue(type, value);
            if (entry.isEmpty()) {
                cacheStore.remove(tenantId, type, value);
                return false;
            }
            if (entry.get().isExpired(LocalDateTime.now())) {
                try {
                    removeFromBlacklist(entry.get().getId());
                } catch (Exception e) {
                    log.warn("failed to lazy-expire blacklist id={}", entry.get().getId(), e);
                }
                return false;
            }
            return true;
        }
        return false;
    }
}

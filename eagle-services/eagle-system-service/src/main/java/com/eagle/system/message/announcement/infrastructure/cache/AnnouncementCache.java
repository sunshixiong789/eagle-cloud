package com.eagle.system.message.announcement.infrastructure.cache;

import com.eagle.system.message.announcement.application.dto.AnnouncementSnapshot;
import com.eagle.system.message.announcement.domain.model.Announcement;
import com.eagle.system.message.announcement.domain.repository.AnnouncementRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告缓存服务——所有用户读取共用。
 *
 * <p>缓存模型（Cache-Aside）：
 * <ul>
 *   <li>{@code announcement:active:v1}：当前所有有效公告的 JSON 列表，TTL {@value #ACTIVE_CACHE_TTL_SECONDS}s</li>
 *   <li>{@code announcement:cursor:v1:{userId}}：用户已读游标 epoch ms，TTL {@value #CURSOR_CACHE_TTL_DAYS}d</li>
 * </ul>
 *
 * <p>读取策略：先 Redis；miss 时回库重建（DB 有 publish_time/expire_time 索引，
 * 即使所有缓存同时失效也不会击穿——单次回库 &lt; 5ms）。
 *
 * <p>失效策略：发布/撤回时主动调 {@link #invalidateActiveCache()}，下次读 miss 重建。
 * 不直接修改缓存数据是因为：
 * <ol>
 *   <li>多实例部署下原子性难保证</li>
 *   <li>"发布/撤回"是低频操作，惰性重建简单可靠</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementCache {

    static final String ACTIVE_KEY = "announcement:active:v1";
    static final String CURSOR_KEY_PREFIX = "announcement:cursor:v1:";
    static final long ACTIVE_CACHE_TTL_SECONDS = 60;
    static final long CURSOR_CACHE_TTL_DAYS = 30;

    private static final TypeReference<List<AnnouncementSnapshot>> SNAPSHOT_LIST_TYPE =
            new TypeReference<>() {};

    private final RedissonClient redissonClient;
    private final AnnouncementRepository announcementRepository;
    private final ObjectMapper objectMapper;

    /**
     * 取所有有效公告快照。多次调用同时 miss 时回库次数 ≤ 实例数（可接受），
     * 真要无锁防击穿可换 {@code CacheProtectionUtil.getWithProtection()}。
     */
    public List<AnnouncementSnapshot> loadActive() {
        RBucket<String> bucket = redissonClient.getBucket(ACTIVE_KEY);
        String cached = bucket.get();
        if (cached != null) {
            return objectMapper.readValue(cached, SNAPSHOT_LIST_TYPE);
        }
        List<Announcement> active = announcementRepository.findAllActive(LocalDateTime.now());
        List<AnnouncementSnapshot> snapshots = active.stream().map(AnnouncementSnapshot::of).toList();
        bucket.set(objectMapper.writeValueAsString(snapshots), Duration.ofSeconds(ACTIVE_CACHE_TTL_SECONDS));
        log.debug("announcement active cache rebuilt: size={}", snapshots.size());
        return snapshots;
    }

    public void invalidateActiveCache() {
        redissonClient.getBucket(ACTIVE_KEY).delete();
        log.debug("announcement active cache invalidated");
    }

    @Nullable
    public LocalDateTime getCursor(Long userId) {
        RBucket<Long> bucket = redissonClient.getBucket(cursorKey(userId));
        Long epochMs = bucket.get();
        return epochMs == null ? null : LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMs), java.time.ZoneId.systemDefault());
    }

    public void setCursor(Long userId, LocalDateTime time) {
        long epochMs = time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        redissonClient.getBucket(cursorKey(userId))
                .set(epochMs, Duration.ofDays(CURSOR_CACHE_TTL_DAYS));
    }

    private String cursorKey(Long userId) {
        return CURSOR_KEY_PREFIX + userId;
    }
}

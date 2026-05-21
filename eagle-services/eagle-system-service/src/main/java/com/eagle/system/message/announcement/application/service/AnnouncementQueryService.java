package com.eagle.system.message.announcement.application.service;

import com.eagle.system.message.announcement.domain.model.UserAnnouncementCursor;
import com.eagle.system.message.announcement.domain.repository.UserAnnouncementCursorRepository;
import com.eagle.system.message.announcement.infrastructure.cache.AnnouncementCache;
import com.eagle.system.message.announcement.infrastructure.cache.AnnouncementSnapshot;
import com.eagle.system.message.announcement.interfaces.dto.AnnouncementView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 公告查询服务（用户视角）。
 *
 * <p>读取链路：Redis active 列表 → 内存过滤受众（按 roles/tags） →
 * 与用户游标比对算已读 → 返回。**单次请求最多 2 次 Redis 读、零 SQL**，
 * 支撑高并发拉公告中心。
 *
 * <p>受众解析依赖调用方传入的 {@code userRoles} / {@code userTags} 集合——
 * 来源应为安全上下文（{@code SecurityUtils}），由 Controller 装配。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementQueryService {

    private final AnnouncementCache announcementCache;
    private final UserAnnouncementCursorRepository cursorRepository;

    /** 当前用户应见公告（按 publish_time 降序），含已读标记。 */
    @Transactional(readOnly = true)
    public List<AnnouncementView> listForUser(Long userId, Set<String> userRoles, Set<String> userTags) {
        List<AnnouncementSnapshot> visible = filterVisible(userRoles, userTags);
        LocalDateTime cursor = resolveCursor(userId);
        return visible.stream()
                .sorted((a, b) -> b.publishTime().compareTo(a.publishTime()))
                .map(s -> AnnouncementView.of(s, !s.publishTime().isAfter(cursor)))
                .toList();
    }

    /** 当前用户的未读公告数。 */
    @Transactional(readOnly = true)
    public long countUnread(Long userId, Set<String> userRoles, Set<String> userTags) {
        List<AnnouncementSnapshot> visible = filterVisible(userRoles, userTags);
        LocalDateTime cursor = resolveCursor(userId);
        return visible.stream()
                .filter(s -> s.publishTime().isAfter(cursor))
                .count();
    }

    /**
     * 推进当前用户游标到最新一条可见公告的 publish_time。
     *
     * <p>若用户当前应见公告中没有比已有游标更新的，{@code save} 也仍执行（保证游标存在），
     * 但不必 {@code advanceTo}。
     */
    @Transactional
    public void markAllRead(Long userId, Set<String> userRoles, Set<String> userTags) {
        List<AnnouncementSnapshot> visible = filterVisible(userRoles, userTags);
        LocalDateTime maxPublishTime = visible.stream()
                .map(AnnouncementSnapshot::publishTime)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        UserAnnouncementCursor cursor = cursorRepository.findByUserId(userId)
                .orElseGet(() -> UserAnnouncementCursor.initial(userId, maxPublishTime));
        cursor.advanceTo(maxPublishTime);
        cursorRepository.save(cursor);
        announcementCache.setCursor(userId, cursor.getLastReadPublishTime());
        log.info("user announcement cursor advanced: userId={}, time={}", userId, cursor.getLastReadPublishTime());
    }

    private List<AnnouncementSnapshot> filterVisible(Set<String> userRoles, Set<String> userTags) {
        Set<String> roles = userRoles == null ? Set.of() : userRoles;
        Set<String> tags = userTags == null ? Set.of() : userTags;
        return announcementCache.loadActive().stream()
                .filter(s -> isVisibleTo(s, roles, tags))
                .toList();
    }

    private boolean isVisibleTo(AnnouncementSnapshot s, Set<String> userRoles, Set<String> userTags) {
        return switch (s.targetType()) {
            case ALL -> true;
            case ROLE -> com.eagle.system.message.announcement.domain.model.TargetFilter
                    .fromJson(s.targetFilterJson()).matchesRoles(userRoles);
            case TAG -> com.eagle.system.message.announcement.domain.model.TargetFilter
                    .fromJson(s.targetFilterJson()).matchesTags(userTags);
        };
    }

    /**
     * 取用户游标——优先 Redis；缓存未命中回 DB；都没有则视为 epoch 起点（=全部未读）。
     */
    private LocalDateTime resolveCursor(Long userId) {
        LocalDateTime cached = announcementCache.getCursor(userId);
        if (cached != null) {
            return cached;
        }
        LocalDateTime fromDb = cursorRepository.findByUserId(userId)
                .map(UserAnnouncementCursor::getLastReadPublishTime)
                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0));
        announcementCache.setCursor(userId, fromDb);
        return fromDb;
    }
}

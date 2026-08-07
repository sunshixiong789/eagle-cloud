package com.eagle.system.message.announcement.domain.model;

import com.eagle.datajpa.base.BaseAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 公告聚合根。
 *
 * <p>一条公告写一行——不论受众多大。已读/未读通过用户级游标
 * {@code UserAnnouncementCursor.getLastReadPublishTime()} 比对 {@code publishTime} 计算，
 * 不维护"用户×公告"的关系表，避免笛卡尔积爆炸。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "announcement", comment = "全员/分组公告表", indexes = {
        @Index(name = "idx_announcement_publish_expire", columnList = "publish_time,expire_time"),
        @Index(name = "idx_announcement_revoked", columnList = "revoked")
})
public class Announcement extends BaseAggregateRoot<Announcement> {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, comment = "分类")
    private AnnouncementCategory category;

    @Column(name = "title", nullable = false, length = 200, comment = "标题")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT", comment = "正文")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20, comment = "受众类型")
    private TargetType targetType;

    @Nullable
    @Column(name = "target_filter", length = 1000, comment = "受众过滤条件 JSON")
    private String targetFilterJson;

    @Column(name = "publish_time", nullable = false, comment = "发布时间")
    private LocalDateTime publishTime;

    @Nullable
    @Column(name = "expire_time", comment = "过期时间，null 表示永久")
    private LocalDateTime expireTime;

    @Column(name = "revoked", nullable = false, comment = "是否已撤回")
    private boolean revoked;

    public static Announcement publish(AnnouncementCategory category, String title, String content,
                                       TargetType targetType, TargetFilter targetFilter,
                                       LocalDateTime publishTime, @Nullable LocalDateTime expireTime) {
        if (publishTime == null) {
            throw AnnouncementErrorCode.ANNOUNCEMENT_INVALID.toDomainException();
        }
        if (expireTime != null && !expireTime.isAfter(publishTime)) {
            throw AnnouncementErrorCode.ANNOUNCEMENT_INVALID.toDomainException();
        }
        Announcement a = new Announcement();
        a.category = category;
        a.title = title;
        a.content = content;
        a.targetType = targetType;
        a.targetFilterJson = targetType == TargetType.ALL ? null
                : (targetFilter == null ? null : targetFilter.toJson());
        a.publishTime = publishTime;
        a.expireTime = expireTime;
        a.revoked = false;
        return a;
    }

    /** 撤回公告——逻辑删除，不物理删行。 */
    public void revoke() {
        if (revoked) {
            return;
        }
        this.revoked = true;
    }

    /**
     * 插入后注册广播事件。
     *
     * <p>必须用 {@code @PostPersist} 而非在应用服务里 save 之后手动调用：Spring Data 的
     * {@code AbstractAggregateRoot} 领域事件由 {@code EventPublishingMethodInterceptor} 在
     * {@code save()} 执行期间抽取并发布，之后清空且不再回看实体。若在 {@code save()} 返回后才
     * {@code registerEvent}，事件永远不会被发布，AFTER_COMMIT 监听器（广播 Publisher）也就不会触发。
     *
     * <p>{@code IDENTITY} 策略下 {@code em.persist} 立即 INSERT 并回填 id，此回调在 {@code save()}
     * 调用栈内触发，id 已可用——单次 save 即可完成持久化 + 事件发布。
     */
    @PostPersist
    private void onPostPersist() {
        registerEvent(new com.eagle.system.message.announcement.domain.event
                .AnnouncementPublishedEvent(getId(), category, targetType, title, content));
    }

    /** 公告对当前请求是否仍有效（未撤回 + 未过期 + 已发布）。 */
    public boolean isActiveAt(LocalDateTime now) {
        if (revoked) {
            return false;
        }
        if (publishTime.isAfter(now)) {
            return false;
        }
        return expireTime == null || expireTime.isAfter(now);
    }

    /** 当前用户是否在受众范围内。 */
    public boolean isVisibleTo(Set<String> userRoles, Set<String> userTags) {
        return switch (targetType) {
            case ALL -> true;
            case ROLE -> getTargetFilter().matchesRoles(userRoles);
            case TAG -> getTargetFilter().matchesTags(userTags);
        };
    }

    public TargetFilter getTargetFilter() {
        return TargetFilter.fromJson(targetFilterJson);
    }
}

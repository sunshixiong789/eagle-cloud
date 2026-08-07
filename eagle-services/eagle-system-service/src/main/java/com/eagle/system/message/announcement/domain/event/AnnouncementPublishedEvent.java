package com.eagle.system.message.announcement.domain.event;

import com.eagle.common.event.BaseEvent;
import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import com.eagle.system.message.announcement.domain.model.TargetType;
import lombok.Getter;

/**
 * 公告发布领域事件。
 *
 * <p>由 {@code AnnouncementAdminApplicationService.publish} 注册到聚合根，事务提交后由
 * {@code AnnouncementBroadcastPublisher} 监听并通过 Redis pub/sub 触发跨实例 WebSocket 广播。
 *
 * <p>携带 {@link #targetType}：实时广播投递策略在基础设施层据此决定——仅 {@link TargetType#ALL}
 * 全员公告走 WebSocket 实时推送；ROLE/TAG 定向公告不上 WS（避免向无关在线用户泄漏），
 * 由客户端经 REST/未读轮询拉取。
 *
 * @author sunshixiong
 */
@Getter
public class AnnouncementPublishedEvent extends BaseEvent {

    private final Long announcementId;
    private final AnnouncementCategory category;
    private final TargetType targetType;
    private final String title;
    private final String content;

    public AnnouncementPublishedEvent(Long announcementId, AnnouncementCategory category,
                                      TargetType targetType, String title, String content) {
        this.announcementId = announcementId;
        this.category = category;
        this.targetType = targetType;
        this.title = title;
        this.content = content;
    }
}

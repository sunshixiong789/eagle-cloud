package com.eagle.system.message.announcement.domain.event;

import com.eagle.common.event.BaseEvent;
import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import lombok.Getter;

/**
 * 公告发布领域事件。
 *
 * <p>由 {@code AnnouncementAdminService.publish} 注册到聚合根，事务提交后由
 * {@code AnnouncementBroadcastPublisher} 监听并通过 Redis pub/sub 触发跨实例 WebSocket 广播。
 *
 * @author sunshixiong
 */
@Getter
public class AnnouncementPublishedEvent extends BaseEvent {

    private final Long announcementId;
    private final AnnouncementCategory category;
    private final String title;
    private final String content;

    public AnnouncementPublishedEvent(Long announcementId, AnnouncementCategory category,
                                      String title, String content) {
        this.announcementId = announcementId;
        this.category = category;
        this.title = title;
        this.content = content;
    }
}

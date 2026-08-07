package com.eagle.system.message.announcement.application.dto;

import com.eagle.system.message.announcement.domain.model.Announcement;
import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import com.eagle.system.message.announcement.domain.model.TargetType;

import java.time.LocalDateTime;

/**
 * 公告缓存快照——只读、可序列化的纯数据载体。
 *
 * <p>不直接缓存 JPA 实体（含代理对象/审计字段，序列化复杂），改缓存本快照。
 * {@code targetFilterJson} 直接以 JSON 字符串保留，复用聚合根的解析逻辑。
 *
 * @author sunshixiong
 */
public record AnnouncementSnapshot(
        Long id,
        AnnouncementCategory category,
        String title,
        String content,
        TargetType targetType,
        String targetFilterJson,
        LocalDateTime publishTime,
        LocalDateTime expireTime
) {

    public static AnnouncementSnapshot of(Announcement a) {
        return new AnnouncementSnapshot(
                a.getId(), a.getCategory(), a.getTitle(), a.getContent(),
                a.getTargetType(), a.getTargetFilter().toJson(),
                a.getPublishTime(), a.getExpireTime()
        );
    }
}

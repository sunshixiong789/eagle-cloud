package com.eagle.system.message.announcement.interfaces.dto;

import com.eagle.system.message.announcement.domain.model.Announcement;
import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import com.eagle.system.message.announcement.domain.model.TargetFilter;
import com.eagle.system.message.announcement.domain.model.TargetType;

import java.time.LocalDateTime;

/**
 * 后台视角的公告 DTO（含撤回状态、过期时间等）。
 *
 * @author sunshixiong
 */
public record AnnouncementAdminView(
        Long id,
        AnnouncementCategory category,
        String title,
        String content,
        TargetType targetType,
        TargetFilter targetFilter,
        LocalDateTime publishTime,
        LocalDateTime expireTime,
        boolean revoked,
        LocalDateTime createTime
) {

    public static AnnouncementAdminView of(Announcement a) {
        return new AnnouncementAdminView(
                a.getId(), a.getCategory(), a.getTitle(), a.getContent(),
                a.getTargetType(), a.getTargetFilter(),
                a.getPublishTime(), a.getExpireTime(),
                a.isRevoked(), a.getCreateTime()
        );
    }
}

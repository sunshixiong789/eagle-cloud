package com.eagle.system.message.announcement.interfaces.dto;

import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import com.eagle.system.message.announcement.application.dto.AnnouncementSnapshot;

import java.time.LocalDateTime;

/**
 * 用户视角的公告 DTO，含已读标记。
 *
 * @author sunshixiong
 */
public record AnnouncementView(
        Long id,
        AnnouncementCategory category,
        String title,
        String content,
        LocalDateTime publishTime,
        LocalDateTime expireTime,
        boolean isRead
) {

    public static AnnouncementView of(AnnouncementSnapshot snapshot, boolean read) {
        return new AnnouncementView(
                snapshot.id(), snapshot.category(),
                snapshot.title(), snapshot.content(),
                snapshot.publishTime(), snapshot.expireTime(),
                read
        );
    }
}


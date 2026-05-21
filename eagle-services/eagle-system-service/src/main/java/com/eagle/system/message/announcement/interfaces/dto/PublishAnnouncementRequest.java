package com.eagle.system.message.announcement.interfaces.dto;

import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import com.eagle.system.message.announcement.domain.model.TargetFilter;
import com.eagle.system.message.announcement.domain.model.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 后台发布公告请求体。
 *
 * @author sunshixiong
 */
public record PublishAnnouncementRequest(
        @NotNull AnnouncementCategory category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @NotNull TargetType targetType,
        @Nullable TargetFilter targetFilter,
        @Nullable LocalDateTime publishTime,
        @Nullable LocalDateTime expireTime
) {
}

package com.eagle.system.message.interfaces.dto;

import com.eagle.system.message.domain.model.MessageCategory;
import com.eagle.system.message.domain.model.UserMessage;

import java.time.LocalDateTime;

/**
 * 站内消息列表 DTO。
 *
 * @author sunshixiong
 */
public record UserMessageResponse(
        Long id,
        MessageCategory category,
        String title,
        String content,
        boolean isRead,
        LocalDateTime createTime
) {

    public static UserMessageResponse of(UserMessage m) {
        return new UserMessageResponse(
                m.getId(), m.getCategory(), m.getTitle(), m.getContent(),
                m.isRead(), m.getCreateTime()
        );
    }
}

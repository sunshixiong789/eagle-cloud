package com.eagle.system.message.domain.model;

import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("UserMessage 聚合根")
class UserMessageTest {

    private static final Long USER_ID = 100L;

    @Test
    @DisplayName("create 初始为未读，承载 bizKey")
    void shouldInitUnread() {
        UserMessage m = UserMessage.create(USER_ID, MessageCategory.SYSTEM, "标题", "正文", "bk-1");
        assertThat(m.getUserId()).isEqualTo(USER_ID);
        assertThat(m.getCategory()).isEqualTo(MessageCategory.SYSTEM);
        assertThat(m.getBizKey()).isEqualTo("bk-1");
        assertThat(m.isRead()).isFalse();
    }

    @Test
    @DisplayName("create 允许 bizKey 为 null")
    void shouldAllowNullBizKey() {
        UserMessage m = UserMessage.create(USER_ID, MessageCategory.TRADE, "t", "c", null);
        assertThat(m.getBizKey()).isNull();
    }

    @Test
    @DisplayName("markRead 设为已读")
    void shouldMarkRead() {
        UserMessage m = UserMessage.create(USER_ID, MessageCategory.SYSTEM, "t", "c", null);
        m.markRead();
        assertThat(m.isRead()).isTrue();
    }

    @Test
    @DisplayName("assertOwnedBy 非本人抛 DomainException")
    void shouldRejectNonOwner() {
        UserMessage m = UserMessage.create(USER_ID, MessageCategory.SYSTEM, "t", "c", null);
        assertThrows(DomainException.class, () -> m.assertOwnedBy(999L));
        assertDoesNotThrow(() -> m.assertOwnedBy(USER_ID));
    }
}

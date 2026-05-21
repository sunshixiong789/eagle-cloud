package com.eagle.system.message.announcement.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserAnnouncementCursor 游标推进")
class UserAnnouncementCursorTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 5, 21, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 5, 21, 12, 0);

    @Test
    @DisplayName("advanceTo 更新到更晚时间 → true，游标推进")
    void advanceForward() {
        UserAnnouncementCursor c = UserAnnouncementCursor.initial(1L, T1);
        boolean advanced = c.advanceTo(T2);
        assertThat(advanced).isTrue();
        assertThat(c.getLastReadPublishTime()).isEqualTo(T2);
    }

    @Test
    @DisplayName("advanceTo 较早时间 → false，游标不变")
    void noBackward() {
        UserAnnouncementCursor c = UserAnnouncementCursor.initial(1L, T2);
        boolean advanced = c.advanceTo(T1);
        assertThat(advanced).isFalse();
        assertThat(c.getLastReadPublishTime()).isEqualTo(T2);
    }

    @Test
    @DisplayName("advanceTo 等于当前时间 → false（保持不变）")
    void equalTimeNoOp() {
        UserAnnouncementCursor c = UserAnnouncementCursor.initial(1L, T1);
        assertThat(c.advanceTo(T1)).isFalse();
    }

    @Test
    @DisplayName("advanceTo null → false")
    void nullSafe() {
        UserAnnouncementCursor c = UserAnnouncementCursor.initial(1L, T1);
        assertThat(c.advanceTo(null)).isFalse();
    }
}

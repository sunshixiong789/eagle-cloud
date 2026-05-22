package com.eagle.system.message.announcement.domain.model;

import com.eagle.datajpa.base.BaseAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户公告读位游标。
 *
 * <p>每用户一行——记录当前用户已读到的最大公告 {@code publishTime}。
 * 公告 {@code publishTime} > 本游标即视为未读。
 *
 * <p>对比"关系表 {@code (announcement_id, user_id, read_at)}"模型：
 * 1 千万用户 × 100 条公告 = 10 亿行，本方案永远 1 亿行（每用户 1 行），
 * 写入 {@code O(1)}，未读判断 {@code O(N)} 内存比较（N = 当前有效公告数，通常 < 100）。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_announcement_cursor", comment = "用户公告读位游标", indexes = {
        @Index(name = "uk_user_announcement_cursor_user", columnList = "user_id", unique = true)
})
public class UserAnnouncementCursor extends BaseAggregateRoot<UserAnnouncementCursor> {

    @Column(name = "user_id", nullable = false, comment = "用户 ID")
    private Long userId;

    @Column(name = "last_read_publish_time", nullable = false,
            comment = "已读到的最大公告 publish_time（含）")
    private LocalDateTime lastReadPublishTime;

    public static UserAnnouncementCursor initial(Long userId, LocalDateTime time) {
        UserAnnouncementCursor c = new UserAnnouncementCursor();
        c.userId = userId;
        c.lastReadPublishTime = time;
        return c;
    }

    /** 推进游标到 {@code time}（仅当更新更大时）。 */
    public boolean advanceTo(LocalDateTime time) {
        if (time == null || !time.isAfter(this.lastReadPublishTime)) {
            return false;
        }
        this.lastReadPublishTime = time;
        return true;
    }
}

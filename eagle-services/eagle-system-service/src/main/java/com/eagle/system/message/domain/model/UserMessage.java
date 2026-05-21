package com.eagle.system.message.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.message.interfaces.exception.MessageErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * 站内消息聚合根（充血模型）。
 *
 * <p>表 {@code user_message} 是平台级共享表，仅含 {@code user_id} 引用，
 * 不与任何业务表 JOIN——保证未来拆出独立 message-service 时整表迁移即可。
 *
 * <p>{@code biz_key} 唯一约束承担幂等去重职责，避免 RocketMQ 重投递造成重复落库。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_message", comment = "站内消息表", indexes = {
        @Index(name = "idx_user_message_user_id", columnList = "user_id"),
        @Index(name = "idx_user_message_user_unread", columnList = "user_id,is_read"),
        @Index(name = "uk_user_message_biz_key", columnList = "biz_key", unique = true)
})
public class UserMessage extends BaseAggregateRoot<UserMessage> {

    @Column(name = "user_id", nullable = false, comment = "接收用户 ID")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, comment = "消息分类")
    private MessageCategory category;

    @Column(name = "title", nullable = false, length = 200, comment = "标题")
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT", comment = "正文")
    private String content;

    @Column(name = "is_read", nullable = false, comment = "是否已读")
    private boolean isRead;

    @Nullable
    @Column(name = "biz_key", length = 128, comment = "业务幂等键")
    private String bizKey;

    /**
     * 创建一条新消息（未读状态），同时注册创建事件以驱动 AFTER_COMMIT 实时推送。
     */
    public static UserMessage create(Long userId, MessageCategory category, String title,
                                     String content, @Nullable String bizKey) {
        UserMessage m = new UserMessage();
        m.userId = userId;
        m.category = category;
        m.title = title;
        m.content = content;
        m.isRead = false;
        m.bizKey = bizKey;
        return m;
    }

    /**
     * 落库后由应用层调用，注册创建事件（事件需要 messageId，必须在 save 后注册）。
     */
    public void registerCreatedEvent() {
        registerEvent(new UserMessageCreatedEvent(getId(), userId, category, title, content));
    }

    public void markRead() {
        this.isRead = true;
    }

    public void assertOwnedBy(Long userId) {
        if (!this.userId.equals(userId)) {
            throw MessageErrorCode.MESSAGE_FORBIDDEN.toDomainException();
        }
    }
}

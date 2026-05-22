package com.eagle.system.auth.domain.model;

import com.eagle.datajpa.base.BaseAggregateRoot;
import com.eagle.system.auth.domain.event.BlacklistAddedEvent;
import com.eagle.system.auth.domain.event.BlacklistRemovedEvent;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 身份黑名单聚合根（全局，不区分租户）。
 *
 * <p>支持 PHONE / EMAIL / IP / ACCOUNT_ID / OPENID 五种黑名单类型，
 * 全局 {@code (type, value)} 唯一。过期时间为 {@code null} 表示永久生效。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "auth_blacklist", indexes = {
        @Index(name = "uk_blacklist_type_value",
                columnList = "type, value", unique = true),
        @Index(name = "idx_blacklist_expires",
                columnList = "expires_at")
})
public class Blacklist extends BaseAggregateRoot<Blacklist> {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, comment = "黑名单类型")
    private BlacklistType type;

    @Column(nullable = false, length = 128, comment = "黑名单值")
    private String value;

    @Column(length = 255, comment = "加黑原因")
    private String reason;

    @Column(name = "expires_at", comment = "过期时间（null=永久）")
    private LocalDateTime expiresAt;

    @Column(name = "operator_id", comment = "操作人ID（null=系统）")
    private Long operatorId;

    @Column(name = "operator_name", length = 64, comment = "操作人姓名")
    private String operatorName;

    /**
     * 创建黑名单条目。
     *
     * @param type         黑名单类型
     * @param value        黑名单值（手机号/IP 等）
     * @param reason       加黑原因，可为 null
     * @param expiresAt    过期时间，null 表示永久
     * @param operatorId   操作人 ID，null 表示系统自动
     * @param operatorName 操作人姓名，可为 null
     * @return 新建的 Blacklist 聚合根（尚未持久化）
     */
    public static Blacklist create(BlacklistType type, String value, String reason,
                                   LocalDateTime expiresAt, Long operatorId, String operatorName) {
        Blacklist b = new Blacklist();
        b.type = type;
        b.value = value;
        b.reason = reason;
        b.expiresAt = expiresAt;
        b.operatorId = operatorId;
        b.operatorName = operatorName;
        return b;
    }

    @PostPersist
    void onPostPersist() {
        registerEvent(new BlacklistAddedEvent(getId(), type, value, expiresAt));
    }

    /**
     * 删除前调用，注册 {@link BlacklistRemovedEvent} 事件（应用服务负责在删除前调用）。
     */
    public void publishRemovedEvent() {
        registerEvent(new BlacklistRemovedEvent(getId(), type, value));
    }

    /**
     * 判断黑名单是否已过期。
     *
     * @param now 当前时间
     * @return {@code true} 表示已过期；{@code false} 表示仍生效（含永久）
     */
    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}

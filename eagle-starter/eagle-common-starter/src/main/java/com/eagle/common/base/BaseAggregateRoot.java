package com.eagle.common.base;

import com.eagle.common.event.BaseDomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根基类
 * <p>
 * 提供领域事件管理能力，所有聚合根都应该继承此类
 *
 * @author sunshixiong
 */
@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAggregateRoot<T extends AbstractAggregateRoot<T>> extends AbstractAggregateRoot<T> {

    /**
     * 领域事件列表（不持久化）
     */
    @Transient
    private final List<BaseDomainEvent> domainEvents = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "主键 ID")
    private Long id;
    @CreatedBy
    @Column(updatable = false, comment = "创建人 ID")
    private Long createBy;
    @LastModifiedBy
    @Column(comment = "更新人 ID")
    private Long updateBy;
    @CreatedDate
    @Column(nullable = false, updatable = false, comment = "创建时间")
    private LocalDateTime createTime;
    @LastModifiedDate
    @Column(comment = "更新时间")
    private LocalDateTime updateTime;
    @Version
    @Column(comment = "乐观锁版本号")
    private Long version;

    @PrePersist
    @PreUpdate
    protected void onUpdateTimestamp() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        updateTime = LocalDateTime.now();
    }

}

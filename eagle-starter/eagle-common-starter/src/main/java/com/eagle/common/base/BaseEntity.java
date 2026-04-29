package com.eagle.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 聚合内子实体基类。
 *
 * <p>提供审计字段（创建人/更新人/时间）和乐观锁，无领域事件能力。
 * 子实体没有独立的 Repository，必须通过聚合根的业务方法进行增删改，由聚合根级联管理。
 *
 * @author sunshixiong
 */
@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

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
}

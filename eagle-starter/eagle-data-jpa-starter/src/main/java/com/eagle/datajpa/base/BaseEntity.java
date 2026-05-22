package com.eagle.datajpa.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 聚合内子实体基类（JPA）。
 *
 * <p>提供审计字段（创建人/更新人/时间）和 {@code @Version} 乐观锁，无领域事件能力。
 *
 * <p><b>使用规范：</b>
 * <ul>
 *   <li>子实体没有独立 Repository，必须通过聚合根的业务方法进行增删改（级联管理）</li>
 *   <li>子实体可暴露 setter，由聚合根调用，但禁止在聚合根外部直接修改子实体状态</li>
 * </ul>
 *
 * <p>R2DBC 场景请改用 {@code com.eagle.r2dbc.base.BaseR2dbcEntity}。
 *
 * @author sunshixiong
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
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

package com.eagle.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 聚合根基类。
 *
 * <p>提供 ID（IDENTITY 策略）、JPA 审计字段、{@code @Version} 乐观锁以及
 * {@link org.springframework.data.domain.AbstractAggregateRoot} 的领域事件注册能力。
 *
 * <p><b>使用规范：</b>
 * <ul>
 *   <li>状态变更必须通过聚合根的业务方法完成，禁止直接暴露 setter</li>
 *   <li>使用静态工厂方法创建实例，禁止 {@code @Builder}（与 Hibernate 代理不兼容）</li>
 *   <li>跨聚合引用只存 ID，禁止 JPA 关联注解跨聚合边界</li>
 * </ul>
 *
 * @param <T> 聚合根自身类型
 * @author sunshixiong
 */
@MappedSuperclass
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAggregateRoot<T extends AbstractAggregateRoot<T>> extends AbstractAggregateRoot<T> {

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

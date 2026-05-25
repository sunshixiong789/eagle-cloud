package com.eagle.r2dbc.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import java.time.LocalDateTime;

/**
 * R2DBC 聚合根 / 实体审计基类。
 *
 * <p>提供审计字段（创建人/更新人/时间）+ 乐观锁版本号。配合
 * {@link com.eagle.r2dbc.config.EagleR2dbcAuditingAutoConfiguration} 提供的
 * {@code ReactiveAuditorAware<Long>} 与 {@code @EnableR2dbcAuditing} 自动填充。
 *
 * <p>字段语义与 {@code BaseEntity}（JPA 版本）一致，便于在 JPA / R2DBC 之间迁移。
 * 但因 R2DBC 不依赖 JPA 注解，不能直接复用 JPA 的 {@code BaseEntity}，
 * 故另立一份单独维护。
 *
 * <p>典型用法：
 * <pre>{@code
 * @Table("t_order")
 * @Getter @Setter @NoArgsConstructor
 * public class Order extends BaseR2dbcEntity {
 *     private String orderNo;
 *     private OrderStatus status;
 *     // ...
 * }
 * }</pre>
 *
 * @author eagle
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseR2dbcEntity {

    @Id
    private Long id;

    @CreatedBy
    private Long createBy;

    @LastModifiedBy
    private Long updateBy;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;

    @Version
    private Long version;
}

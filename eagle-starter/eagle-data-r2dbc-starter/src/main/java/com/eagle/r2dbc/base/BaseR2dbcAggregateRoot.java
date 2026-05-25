package com.eagle.r2dbc.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;

/**
 * R2DBC 聚合根基类。
 *
 * <p>提供：
 * <ul>
 *   <li>主键 ID（{@link Id}，由数据库自增或 starter 内部 ID 生成器分配）</li>
 *   <li>审计字段（创建人 / 更新人 / 时间）— 由 {@code @EnableR2dbcAuditing}
 *       + {@code ReactiveAuditorAware} 自动填充</li>
 *   <li>{@code @Version} 乐观锁（Spring Data R2DBC 在 {@code save()} 时自动比对并自增）</li>
 *   <li>{@link AbstractAggregateRoot#registerEvent(Object)} 领域事件能力 —
 *       {@code R2dbcRepository.save()} 后由 {@code EventPublishingRepositoryProxyPostProcessor}
 *       自动发布至 {@code ApplicationEventPublisher}，行为与 JPA 端 {@code BaseAggregateRoot} 完全一致</li>
 * </ul>
 *
 * <p><b>使用规范：</b>
 * <ul>
 *   <li>状态变更必须通过聚合根的业务方法完成，禁止直接暴露 setter</li>
 *   <li>使用静态工厂方法创建实例</li>
 *   <li>跨聚合引用只存 ID，禁止通过关联对象引用其他聚合</li>
 * </ul>
 *
 * <p>典型用法：
 * <pre>{@code
 * @Table("t_order")
 * @Getter @NoArgsConstructor
 * public class Order extends BaseR2dbcAggregateRoot<Order> {
 *
 *     private String orderNo;
 *     private OrderStatus status;
 *
 *     public static Order create(String orderNo) {
 *         Order order = new Order();
 *         order.orderNo = orderNo;
 *         order.status = OrderStatus.CREATED;
 *         order.registerEvent(new OrderCreatedEvent(orderNo));
 *         return order;
 *     }
 *
 *     public void pay() {
 *         this.status = OrderStatus.PAID;
 *         registerEvent(new OrderPaidEvent(getId(), orderNo));
 *     }
 * }
 * }</pre>
 *
 * @param <T> 聚合根自身类型
 * @author eagle
 */
@Getter
@NoArgsConstructor
public abstract class BaseR2dbcAggregateRoot<T extends BaseR2dbcAggregateRoot<T>>
        extends AbstractAggregateRoot<T> {

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

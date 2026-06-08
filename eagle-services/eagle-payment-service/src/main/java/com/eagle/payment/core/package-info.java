/**
 * Eagle 支付服务核心域。
 *
 * <p>四层结构 (DDD):
 * <ul>
 *   <li>{@code interfaces/} - REST 控制器 + DTO</li>
 *   <li>{@code application/} - 应用服务 (用例编排 / 事务边界) + Mapper</li>
 *   <li>{@code domain/} - 聚合根 / 值对象 / 领域事件 / 出站端口 (无框架依赖)</li>
 *   <li>{@code infrastructure/} - 网关适配器 / Repository / 事件分发 / 配置</li>
 * </ul>
 *
 * <p>子域 (Payment / Refund / Transfer / Reconcile) 共享同一 com.eagle.payment.core
 * 包,但内部按聚合根隔离 (model/aggregate, repository, port)。未来若任一子域有独立
 * 演化诉求,可下钻为 com.eagle.payment.refund / com.eagle.payment.transfer 子包并加
 * {@code @ApplicationModule} 声明边界。
 */
@NullMarked
package com.eagle.payment.core;

import org.jspecify.annotations.NullMarked;

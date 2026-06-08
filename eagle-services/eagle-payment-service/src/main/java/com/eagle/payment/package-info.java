/**
 * Eagle Payment Service 应用根包。
 *
 * <p>本包仅承载 Spring Boot 应用入口 {@code EaglePaymentApplication} 与
 * {@code @Modulithic} 声明,不包含业务代码。业务代码位于
 * {@link com.eagle.payment.core} 子包(单一有界上下文:支付收款 / 退款 / 提现 / 对账)。
 *
 * <p>选择"应用基础包 ≠ 模块基础包"的拓扑,是为了让
 * {@code ApplicationModules.of(EaglePaymentApplication.class).verify()} 能将
 * {@code com.eagle.payment.core} 识别为单一模块,而不是把 DDD 四层误判为
 * 四个独立模块导致循环依赖告警。
 *
 * @author sunshixiong
 */
@NullMarked
package com.eagle.payment;

import org.jspecify.annotations.NullMarked;

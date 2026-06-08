package com.eagle.payment.core.domain.model.enums;

/**
 * 支付渠道枚举。
 *
 * <p>区分上层业务路由到哪一个 {@link com.eagle.payment.core.domain.port.PaymentGatewayPort}
 * 实现。新增渠道需:
 * <ol>
 *   <li>在此枚举追加常量;</li>
 *   <li>新增 {@code infrastructure/gateway/xxx} 目录,实现 {@code PaymentGatewayPort}
 *       且 {@code getChannel()} 返回对应枚举;</li>
 *   <li>注册 Spring Bean 并通过 {@code @ConditionalOnProperty} 守门。</li>
 * </ol>
 *
 * @author sunshixiong
 */
public enum PaymentChannel {
    /** 支付宝。 */
    ALIPAY,
    /** 微信支付。 */
    WECHAT
}

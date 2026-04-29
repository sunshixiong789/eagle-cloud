package com.eagle.idempotency.annotation;

/**
 * 幂等性控制模式枚举。
 *
 * @author sunshixiong
 */
public enum IdempotencyMode {

    /**
     * Token 模式：客户端预先调用 {@code GET /idempotency/token} 获取一次性 Token，
     * 请求时通过 Header 携带，服务端原子性校验并消费该 Token。
     * <p>适用于：下单、支付等需严格防重的写操作。
     */
    TOKEN,

    /**
     * 业务键模式：基于业务唯一标识（如订单号、流水号）通过 SpEL 表达式提取，
     * Redis setNX 保证同一业务键在指定时间窗口内只处理一次。
     * <p>适用于：幂等性要求基于自身业务语义的场景，如消息消费防重。
     */
    BUSINESS_KEY,

    /**
     * 结果缓存模式：首次执行成功后缓存响应结果，重复请求直接返回缓存结果，不报错。
     * <p>与 {@link #TOKEN} 模式不同，RESULT_CACHE 对幂等重试友好——重复携带相同 Token
     * 的请求不会因 Token 已消费而报错，而是直接返回首次执行的结果。
     * <p>适用于：支付回调、订单提交等需要对幂等重试友好的场景，
     * 第三方系统因网络超时重试时可直接命中缓存，不重复执行业务逻辑。
     */
    RESULT_CACHE
}

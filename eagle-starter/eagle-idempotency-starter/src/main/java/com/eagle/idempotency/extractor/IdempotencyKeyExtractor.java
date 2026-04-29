package com.eagle.idempotency.extractor;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 幂等业务键提取器 SPI 接口。
 *
 * <p>当 {@link com.eagle.idempotency.annotation.Idempotent#mode()} 为
 * {@link com.eagle.idempotency.annotation.IdempotencyMode#BUSINESS_KEY} 时，
 * 可实现此接口替代 SpEL 表达式，用于复杂键提取逻辑（如联合请求头 + 请求体计算键、
 * 调用远程服务获取唯一键等）。
 *
 * <p>实现类须注册为 Spring Bean，并在 {@code @Idempotent(keyExtractor = "myExtractor")}
 * 中填写对应的 Bean 名称。当 {@code keyExtractor} 非空时，优先于 {@code key()} SpEL 表达式使用。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Component("orderIdempotencyKeyExtractor")
 * public class OrderIdempotencyKeyExtractor implements IdempotencyKeyExtractor {
 *
 *     @Override
 *     public String extract(ProceedingJoinPoint joinPoint) {
 *         Object[] args = joinPoint.getArgs();
 *         CreateOrderRequest request = (CreateOrderRequest) args[0];
 *         // 组合多字段构造唯一业务键
 *         return request.getUserId() + ":" + request.getProductId() + ":" + request.getAmount();
 *     }
 * }
 *
 * // 在 Controller 中使用
 * @PostMapping("/orders")
 * @Idempotent(mode = IdempotencyMode.BUSINESS_KEY, keyExtractor = "orderIdempotencyKeyExtractor")
 * public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) { ... }
 * }</pre>
 *
 * @author sunshixiong
 */
public interface IdempotencyKeyExtractor {

    /**
     * 提取业务幂等键。
     *
     * <p>实现须保证在相同业务语义下返回相同的键字符串，幂等键相同的重复请求将被拦截。
     *
     * @param joinPoint 切点，可从中获取方法参数（{@link ProceedingJoinPoint#getArgs()}）、
     *                  方法签名（{@link ProceedingJoinPoint#getSignature()}）等信息
     * @return 幂等键字符串，不可为 {@code null} 或空字符串
     */
    String extract(ProceedingJoinPoint joinPoint);
}

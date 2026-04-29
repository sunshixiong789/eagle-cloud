package com.eagle.idempotency.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记请求对象中的幂等键字段。
 *
 * <p>在 {@link com.eagle.idempotency.annotation.IdempotencyMode#BUSINESS_KEY} 模式下，
 * 当 {@link Idempotent#key()} 为空且 {@link Idempotent#keyExtractor()} 也为空时，
 * 切面会自动扫描切点第一个参数对象中标有此注解的字段，提取其值并拼接为幂等键。
 *
 * <p>若对象中有多个字段标注此注解，所有字段值将按字段声明顺序拼接，
 * 格式为 {@code "{fieldName}:{value}|{fieldName}:{value}"}。
 *
 * <p>使用示例：
 * <pre>{@code
 * public class CreateOrderRequest {
 *
 *     // 标注单字段：幂等键 = "orderNo:{实际值}"
 *     @IdempotencyKey
 *     private String orderNo;
 *
 *     private String productName;
 * }
 *
 * // 多字段组合（使用 prefix 区分）：
 * public class PayRequest {
 *
 *     @IdempotencyKey(prefix = "userId")
 *     private Long userId;
 *
 *     @IdempotencyKey(prefix = "tradeNo")
 *     private String tradeNo;
 * }
 * // 生成的幂等键格式：userId:{value}|tradeNo:{value}
 * }</pre>
 *
 * @author sunshixiong
 * @see Idempotent
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotencyKey {

    /**
     * 键前缀，用于多字段组合时区分各字段语义。
     * <p>若为空，则使用字段名作为前缀。
     */
    String prefix() default "";
}

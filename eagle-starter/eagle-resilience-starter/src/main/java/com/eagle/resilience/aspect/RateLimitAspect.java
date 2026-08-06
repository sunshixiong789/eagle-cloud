package com.eagle.resilience.aspect;

import com.eagle.resilience.annotation.RateLimit;
import com.eagle.resilience.annotation.RateLimitBehavior;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.time.Duration;

/**
 * {@link RateLimit} 注解的 AOP 切面实现（Resilience4J 后端）。
 *
 * <p>拦截所有标注了 {@code @RateLimit} 的方法（或类），按资源名从
 * {@link RateLimiterRegistry} 取（首次则按注解配置创建）限流器，
 * 拿不到令牌时抛出 {@code RequestNotPermitted}。
 *
 * <p>{@link RateLimit#threads()} &gt; 0 时额外套一层信号量 {@link Bulkhead} 限制并发，
 * 满载时抛出 {@code BulkheadFullException}。两者均由
 * {@link com.eagle.resilience.handler.RateLimitExceptionHandler} 转成 HTTP 429。
 *
 * <p>切点优先级：方法级注解优先于类级注解。
 * 资源名规则：{@link RateLimit#resource()} 非空则使用指定值，
 * 否则自动生成 {@code 简单类名.方法名}。
 *
 * <p>本切面取代原 {@code com.eagle.sentinel.aspect.RateLimitAspect}，
 * 行为差异见 {@link RateLimitBehavior} 文档（不再支持 WARM_UP）。
 *
 * @author eagle
 * @see RateLimit
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    /**
     * 限流刷新周期固定 1 秒 —— {@code qps} 的语义即"每秒许可数"
     */
    private static final Duration REFRESH_PERIOD = Duration.ofSeconds(1);

    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    /**
     * 拦截方法级 {@link RateLimit} 注解。
     *
     * @param joinPoint 切点信息
     * @param rateLimit 方法上的注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的任意异常，或限流触发时的 {@code RequestNotPermitted}
     */
    @Around("@annotation(rateLimit)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return executeWithRateLimit(joinPoint, rateLimit, resolveResourceName(joinPoint, rateLimit));
    }

    /**
     * 拦截类级 {@link RateLimit} 注解（覆盖类中所有方法）。
     *
     * <p>类级注解时各方法资源名独立计算（忽略 {@code resource} 属性），
     * 使同一个类里的不同方法各自计量，与原 Sentinel 实现保持一致。
     *
     * @param joinPoint 切点信息
     * @param rateLimit 类上的注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的任意异常，或限流触发时的 {@code RequestNotPermitted}
     */
    @Around("@within(rateLimit) && !@annotation(com.eagle.resilience.annotation.RateLimit)")
    public Object aroundClass(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return executeWithRateLimit(joinPoint, rateLimit, buildDefaultResourceName(joinPoint));
    }

    /**
     * 申请令牌（及并发许可）后执行目标方法。
     *
     * @param joinPoint    切点信息
     * @param rateLimit    限流注解配置
     * @param resourceName 资源名，同时作为 Resilience4J 实例名
     * @return 目标方法返回值
     * @throws Throwable 目标方法或框架抛出的异常
     */
    private Object executeWithRateLimit(ProceedingJoinPoint joinPoint,
                                        RateLimit rateLimit, String resourceName) throws Throwable {
        // registry 按名字缓存实例，Supplier 只在首次创建时求值
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(
                resourceName, () -> buildRateLimiterConfig(rateLimit));

        // 拿不到令牌抛 RequestNotPermitted，交由 RateLimitExceptionHandler 转 429
        RateLimiter.waitForPermission(rateLimiter);

        if (rateLimit.threads() <= 0) {
            return joinPoint.proceed();
        }

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(
                resourceName, () -> buildBulkheadConfig(rateLimit));
        // 满载抛 BulkheadFullException，同样转 429
        bulkhead.acquirePermission();
        try {
            return joinPoint.proceed();
        } finally {
            bulkhead.onComplete();
        }
    }

    /**
     * 按注解构建 Resilience4J 限流器配置。
     *
     * @param rateLimit 限流注解配置
     * @return RateLimiter 配置
     */
    private RateLimiterConfig buildRateLimiterConfig(RateLimit rateLimit) {
        // Resilience4J 许可数为整型，小数 qps 向下取整；至少放行 1 个避免配置失误导致全量拒绝
        int limitForPeriod = Math.max(1, (int) rateLimit.qps());
        Duration timeout = rateLimit.behavior() == RateLimitBehavior.QUEUEING
                ? Duration.ofMillis(rateLimit.maxQueueingTimeMs())
                : Duration.ZERO;

        log.info("[RateLimit] RateLimiter created: resource={}, qps={}, behavior={}, timeout={}ms",
                rateLimit.resource(), limitForPeriod, rateLimit.behavior(), timeout.toMillis());

        return RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(REFRESH_PERIOD)
                .timeoutDuration(timeout)
                .build();
    }

    /**
     * 按注解构建 Resilience4J 信号量隔离配置（仅 {@code threads > 0} 时使用）。
     *
     * @param rateLimit 限流注解配置
     * @return Bulkhead 配置
     */
    private BulkheadConfig buildBulkheadConfig(RateLimit rateLimit) {
        log.info("[RateLimit] Bulkhead created: resource={}, maxConcurrentCalls={}",
                rateLimit.resource(), rateLimit.threads());

        return BulkheadConfig.custom()
                .maxConcurrentCalls(rateLimit.threads())
                // 并发维度不排队：QPS 维度已经承担了削峰职责
                .maxWaitDuration(Duration.ZERO)
                .build();
    }

    /**
     * 解析最终使用的资源名称。
     *
     * @param joinPoint 切点信息
     * @param rateLimit 限流注解
     * @return 资源名
     */
    private String resolveResourceName(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimit.resource().isBlank()) {
            return rateLimit.resource();
        }
        return buildDefaultResourceName(joinPoint);
    }

    /**
     * 构建默认资源名：{@code 简单类名.方法名}。
     *
     * @param joinPoint 切点信息
     * @return 默认资源名
     */
    private String buildDefaultResourceName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className + "." + signature.getMethod().getName();
    }
}

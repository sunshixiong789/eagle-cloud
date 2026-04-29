package com.eagle.sentinel.aspect;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.eagle.sentinel.annotation.FlowControlBehavior;
import com.eagle.sentinel.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RateLimit} 注解的 AOP 切面实现。
 *
 * <p>拦截所有标注了 {@code @RateLimit} 的方法（或类），
 * 首次调用时向 Sentinel {@link FlowRuleManager} 动态注册流控规则，
 * 之后通过 {@link SphU#entry} 进入 Sentinel 资源保护。
 *
 * <p>切点优先级：方法级注解优先于类级注解。
 * 资源名规则：{@link RateLimit#resource()} 非空则使用指定值，
 * 否则自动生成 {@code 简单类名.方法名}。
 *
 * @author 孙士雄
 * @see RateLimit
 * @see FlowRuleManager
 */
@Slf4j
@Aspect
public class RateLimitAspect {

    /**
     * 已注册规则的资源名集合，避免重复注册（内存规则在重复 loadRules 时会覆盖已有规则）。
     */
    private final Map<String, Boolean> registeredResources = new ConcurrentHashMap<>();

    /**
     * 拦截方法级 {@link RateLimit} 注解。
     *
     * @param joinPoint 切点信息
     * @param rateLimit 方法上的注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的任意异常，或 {@link BlockException} 被限流时抛出
     */
    @Around("@annotation(rateLimit)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String resourceName = resolveResourceName(joinPoint, rateLimit);
        return executeWithRateLimit(joinPoint, rateLimit, resourceName);
    }

    /**
     * 拦截类级 {@link RateLimit} 注解（覆盖类中所有方法）。
     *
     * <p>当类上标注了 {@code @RateLimit} 时，类中每个方法的资源名独立计算，
     * 格式为 {@code 简单类名.方法名}（或注解中显式指定的 resource 值）。
     *
     * @param joinPoint 切点信息
     * @param rateLimit 类上的注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的任意异常，或 {@link BlockException} 被限流时抛出
     */
    @Around("@within(rateLimit) && !@annotation(com.eagle.sentinel.annotation.RateLimit)")
    public Object aroundClass(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 类级注解时，资源名取方法全限定名（忽略 resource 属性，各方法独立计量）
        String resourceName = buildDefaultResourceName(joinPoint);
        return executeWithRateLimit(joinPoint, rateLimit, resourceName);
    }

    /**
     * 进入 Sentinel 资源保护并执行目标方法。
     *
     * <p>首次调用时注册流控规则；每次调用通过 {@link SphU#entry} 申请令牌，
     * 若触发限流则重新抛出 {@link BlockException}，由上游的
     * {@link com.eagle.sentinel.handler.EagleSentinelBlockExceptionHandler} 处理。
     *
     * @param joinPoint    切点信息
     * @param rateLimit    限流注解配置
     * @param resourceName Sentinel 资源名
     * @return 目标方法返回值
     * @throws Throwable 目标方法或框架抛出的异常
     */
    private Object executeWithRateLimit(ProceedingJoinPoint joinPoint,
            RateLimit rateLimit, String resourceName) throws Throwable {
        // 首次调用时注册规则（ConcurrentHashMap 保证并发安全）
        registeredResources.computeIfAbsent(resourceName, key -> {
            registerFlowRule(key, rateLimit);
            return Boolean.TRUE;
        });

        Entry entry = null;
        try {
            entry = SphU.entry(resourceName, EntryType.IN);
            return joinPoint.proceed();
        } catch (BlockException ex) {
            // 重新抛出，由 EagleSentinelBlockExceptionHandler 统一处理
            log.debug("[RateLimit] Resource '{}' blocked: {}", resourceName, ex.getClass().getSimpleName());
            throw ex;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 动态向 Sentinel FlowRuleManager 注册流控规则。
     *
     * <p>规则注册采用合并策略：取出当前已有规则列表，追加新规则后重新 load，
     * 避免覆盖其他资源已注册的规则。
     *
     * @param resourceName 资源名
     * @param rateLimit    限流注解配置
     */
    private void registerFlowRule(String resourceName, RateLimit rateLimit) {
        FlowRule rule = new FlowRule(resourceName);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(rateLimit.qps());
        rule.setControlBehavior(mapControlBehavior(rateLimit.behavior()));

        if (rateLimit.behavior() == FlowControlBehavior.WARM_UP) {
            rule.setWarmUpPeriodSec(rateLimit.warmUpPeriodSec());
        }
        if (rateLimit.behavior() == FlowControlBehavior.RATE_LIMITER) {
            rule.setMaxQueueingTimeMs(rateLimit.maxQueueingTimeMs());
        }

        // 追加到现有规则列表，避免清除已有资源的规则
        List<FlowRule> rules = new ArrayList<>(FlowRuleManager.getRules());
        rules.add(rule);
        FlowRuleManager.loadRules(rules);

        log.info("[RateLimit] Flow rule registered: resource={}, qps={}, behavior={}",
                resourceName, rateLimit.qps(), rateLimit.behavior());
    }

    /**
     * 将 {@link FlowControlBehavior} 映射为 Sentinel {@code RuleConstant} 中的控制行为常量。
     *
     * @param behavior 流控行为枚举
     * @return Sentinel 控制行为常量值
     */
    private int mapControlBehavior(FlowControlBehavior behavior) {
        return switch (behavior) {
            case WARM_UP -> RuleConstant.CONTROL_BEHAVIOR_WARM_UP;
            case RATE_LIMITER -> RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER;
            default -> RuleConstant.CONTROL_BEHAVIOR_DEFAULT;
        };
    }

    /**
     * 解析最终使用的资源名称。
     *
     * <p>若注解中 {@link RateLimit#resource()} 非空则直接使用，
     * 否则生成默认名 {@code 简单类名.方法名}。
     *
     * @param joinPoint 切点信息
     * @param rateLimit 限流注解
     * @return 资源名
     */
    private String resolveResourceName(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (rateLimit.resource() != null && !rateLimit.resource().isBlank()) {
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
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className + "." + method.getName();
    }
}

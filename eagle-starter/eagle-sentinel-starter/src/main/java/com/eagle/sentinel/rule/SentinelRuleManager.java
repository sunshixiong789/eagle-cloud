package com.eagle.sentinel.rule;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sentinel 规则程序化管理器。
 *
 * <p>提供在代码中动态注册和清除 Sentinel 限流/熔断/热点参数规则的便捷 API，
 * 适用于启动时批量加载规则、运行时根据业务条件动态调整规则等场景。
 *
 * <p>使用示例：
 * <pre>{@code
 * @PostConstruct
 * public void initRules() {
 *     sentinelRuleManager.addFlowRule("createOrder", 100);
 *     sentinelRuleManager.addSlowCallDegradeRule("queryOrder", 0.5, 500, 10);
 *     sentinelRuleManager.addParamFlowRule("queryProduct", 0, 50);
 * }
 * }</pre>
 *
 * <p>所有方法均采用追加（merge）策略，不会清除已有资源的规则。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
public class SentinelRuleManager {

    /**
     * 添加或更新 QPS 流控规则。
     *
     * <p>若同一资源已存在流控规则，则追加新规则（同一资源允许多条规则，
     * 取最严格的阈值生效）。
     *
     * @param resource 资源名，不得为空
     * @param qps      每秒请求数阈值，超过则触发限流
     */
    public void addFlowRule(String resource, double qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setCount(qps);

        List<FlowRule> rules = new ArrayList<>(FlowRuleManager.getRules());
        rules.add(rule);
        FlowRuleManager.loadRules(rules);

        log.info("[SentinelRuleManager] Flow rule added: resource={}, qps={}", resource, qps);
    }

    /**
     * 添加慢调用响应时间熔断规则。
     *
     * <p>当慢调用（响应时间超过 {@code rtThresholdMs}）的比例超过 {@code slowRatioThreshold} 时，
     * 触发熔断，持续 {@code timeWindowSec} 秒后进入半开探测状态。
     *
     * @param resource           资源名，不得为空
     * @param slowRatioThreshold 慢调用比例阈值（0~1），如 0.5 表示 50% 慢调用时熔断
     * @param rtThresholdMs      慢调用响应时间阈值（毫秒），超过此值视为慢调用
     * @param timeWindowSec      熔断持续时长（秒），期间所有请求直接失败
     */
    public void addSlowCallDegradeRule(String resource, double slowRatioThreshold,
            long rtThresholdMs, int timeWindowSec) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType());
        rule.setCount(slowRatioThreshold);
        rule.setSlowRatioThreshold(slowRatioThreshold);
        rule.setStatIntervalMs(1000);
        rule.setMinRequestAmount(5);
        rule.setTimeWindow(timeWindowSec);
        // 使用 count 字段存储 RT 阈值（Sentinel DegradeRule 的语义）
        rule.setCount(rtThresholdMs);
        rule.setSlowRatioThreshold(slowRatioThreshold);

        List<DegradeRule> rules = new ArrayList<>(DegradeRuleManager.getRules());
        rules.add(rule);
        DegradeRuleManager.loadRules(rules);

        log.info("[SentinelRuleManager] Slow call degrade rule added: resource={}, slowRatioThreshold={}, "
                        + "rtThresholdMs={}, timeWindowSec={}",
                resource, slowRatioThreshold, rtThresholdMs, timeWindowSec);
    }

    /**
     * 添加异常比例熔断规则。
     *
     * <p>当单位统计窗口内异常比例超过 {@code exceptionRatioThreshold} 时，
     * 触发熔断，持续 {@code timeWindowSec} 秒。
     *
     * @param resource                资源名，不得为空
     * @param exceptionRatioThreshold 异常比例阈值（0~1），如 0.5 表示 50% 异常时熔断
     * @param timeWindowSec           熔断持续时长（秒）
     */
    public void addExceptionRatioDegradeRule(String resource, double exceptionRatioThreshold,
            int timeWindowSec) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
        rule.setCount(exceptionRatioThreshold);
        rule.setStatIntervalMs(1000);
        rule.setMinRequestAmount(5);
        rule.setTimeWindow(timeWindowSec);

        List<DegradeRule> rules = new ArrayList<>(DegradeRuleManager.getRules());
        rules.add(rule);
        DegradeRuleManager.loadRules(rules);

        log.info("[SentinelRuleManager] Exception ratio degrade rule added: resource={}, "
                        + "exceptionRatioThreshold={}, timeWindowSec={}",
                resource, exceptionRatioThreshold, timeWindowSec);
    }

    /**
     * 添加热点参数限流规则。
     *
     * <p>针对方法的某个参数进行限流，不同参数值的计数独立统计。
     * 适用于对热门商品、热门用户等特定参数值的限流保护。
     *
     * @param resource 资源名，不得为空
     * @param paramIdx 参数索引（从 0 开始），指定对哪个参数进行热点限流
     * @param qps      该参数维度的 QPS 阈值
     */
    public void addParamFlowRule(String resource, int paramIdx, double qps) {
        ParamFlowRule rule = new ParamFlowRule(resource);
        rule.setParamIdx(paramIdx);
        rule.setCount(qps);

        List<ParamFlowRule> rules = new ArrayList<>(ParamFlowRuleManager.getRulesOfResource(resource));
        rules.add(rule);
        ParamFlowRuleManager.loadRules(rules);

        log.info("[SentinelRuleManager] Param flow rule added: resource={}, paramIdx={}, qps={}",
                resource, paramIdx, qps);
    }

    /**
     * 清除指定资源的所有流控规则。
     *
     * <p>仅清除 QPS 流控规则，熔断规则和热点参数规则不受影响。
     * 适用于动态撤销限流配置的场景。
     *
     * @param resource 资源名，不得为空
     */
    public void clearFlowRules(String resource) {
        List<FlowRule> remaining = FlowRuleManager.getRules().stream()
                .filter(rule -> !resource.equals(rule.getResource()))
                .collect(Collectors.toList());
        FlowRuleManager.loadRules(remaining);

        log.info("[SentinelRuleManager] Flow rules cleared: resource={}", resource);
    }
}

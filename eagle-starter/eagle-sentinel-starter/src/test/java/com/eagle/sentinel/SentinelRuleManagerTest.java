package com.eagle.sentinel;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.eagle.sentinel.rule.SentinelRuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SentinelRuleManager} 单元测试。
 *
 * <p>直接实例化被测类，调用 Sentinel 静态 RuleManager（测试 classpath 已含 sentinel-core）。
 * 每个测试前清空所有规则，避免测试间相互干扰。
 */
@DisplayName("SentinelRuleManager")
class SentinelRuleManagerTest {

    private SentinelRuleManager manager;

    @BeforeEach
    void setUp() {
        // 清空所有静态规则，保证测试隔离
        FlowRuleManager.loadRules(List.of());
        DegradeRuleManager.loadRules(List.of());
        ParamFlowRuleManager.loadRules(List.of());
        manager = new SentinelRuleManager();
    }

    @Nested
    @DisplayName("addFlowRule")
    class AddFlowRule {

        @Test
        @DisplayName("shouldAddFlowRule — 添加后 FlowRuleManager 包含该资源的规则")
        void shouldAddFlowRule() {
            manager.addFlowRule("testResource", 100);

            List<FlowRule> rules = FlowRuleManager.getRules();
            assertFalse(rules.isEmpty(), "规则列表不应为空");
            boolean found = rules.stream()
                    .anyMatch(r -> "testResource".equals(r.getResource()) && r.getCount() == 100);
            assertTrue(found, "应存在 testResource QPS=100 的流控规则");
        }

        @Test
        @DisplayName("shouldAddFlowRuleWithoutClearingOthers — 添加 resource-B 规则时 resource-A 规则不受影响")
        void shouldAddFlowRuleWithoutClearingOthers() {
            manager.addFlowRule("resource-A", 50);
            manager.addFlowRule("resource-B", 200);

            List<FlowRule> rules = FlowRuleManager.getRules();
            boolean hasA = rules.stream().anyMatch(r -> "resource-A".equals(r.getResource()));
            boolean hasB = rules.stream().anyMatch(r -> "resource-B".equals(r.getResource()));
            assertTrue(hasA, "resource-A 的规则应保留");
            assertTrue(hasB, "resource-B 的规则应存在");
        }
    }

    @Nested
    @DisplayName("addSlowCallDegradeRule")
    class AddSlowCallDegradeRule {

        @Test
        @DisplayName("shouldAddDegradeRule — 添加后 DegradeRuleManager 包含该资源的熔断规则")
        void shouldAddDegradeRule() {
            manager.addSlowCallDegradeRule("slowResource", 0.5, 500, 10);

            List<DegradeRule> rules = DegradeRuleManager.getRules();
            assertFalse(rules.isEmpty(), "熔断规则列表不应为空");
            boolean found = rules.stream()
                    .anyMatch(r -> "slowResource".equals(r.getResource())
                            && r.getTimeWindow() == 10);
            assertTrue(found, "应存在 slowResource 的慢调用熔断规则，窗口为 10s");
        }
    }

    @Nested
    @DisplayName("clearFlowRules")
    class ClearFlowRules {

        @Test
        @DisplayName("shouldClearFlowRulesForResource — 清除后该资源规则消失，其他资源规则不受影响")
        void shouldClearFlowRulesForResource() {
            manager.addFlowRule("keep-resource", 80);
            manager.addFlowRule("remove-resource", 40);

            manager.clearFlowRules("remove-resource");

            List<FlowRule> rules = FlowRuleManager.getRules();
            boolean removedResourceGone = rules.stream()
                    .noneMatch(r -> "remove-resource".equals(r.getResource()));
            boolean keepResourceExists = rules.stream()
                    .anyMatch(r -> "keep-resource".equals(r.getResource()));
            assertTrue(removedResourceGone, "remove-resource 的规则应已被清除");
            assertTrue(keepResourceExists, "keep-resource 的规则应保持不变");
        }

        @Test
        @DisplayName("should不执行操作WhenResourceHasNoRules — 清除不存在的资源不抛异常")
        void shouldDoNothingWhenResourceHasNoRules() {
            manager.addFlowRule("existing-resource", 100);

            // 清除一个不存在的资源，不应抛出异常，也不应影响已有规则
            manager.clearFlowRules("non-existent-resource");

            List<FlowRule> rules = FlowRuleManager.getRules();
            assertEquals(1, rules.size(), "规则总数应保持不变");
            assertTrue(rules.stream().anyMatch(r -> "existing-resource".equals(r.getResource())));
        }
    }

    @Nested
    @DisplayName("add参数FlowRule")
    class AddParamFlowRule {

        @Test
        @DisplayName("shouldAdd参数FlowRule — 添加后 参数FlowRuleManager 包含该资源的热点规则")
        void shouldAddParamFlowRule() {
            manager.addParamFlowRule("hotResource", 0, 50);

            List<ParamFlowRule> rules = ParamFlowRuleManager.getRulesOfResource("hotResource");
            assertFalse(rules.isEmpty(), "热点参数规则列表不应为空");
            ParamFlowRule rule = rules.get(0);
            assertEquals("hotResource", rule.getResource());
            assertEquals(0, rule.getParamIdx());
            assertEquals(50, rule.getCount(), 0.001);
        }

        @Test
        @DisplayName("shouldAddMultiple参数RulesForSameResource — 同一资源可叠加多条热点规则")
        void shouldAddMultipleParamRulesForSameResource() {
            manager.addParamFlowRule("multiParamResource", 0, 100);
            manager.addParamFlowRule("multiParamResource", 1, 200);

            List<ParamFlowRule> rules = ParamFlowRuleManager.getRulesOfResource("multiParamResource");
            assertEquals(2, rules.size(), "同一资源的热点规则应叠加");
        }
    }
}

package com.eagle.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.eagle.sentinel.annotation.FlowControlBehavior;
import com.eagle.sentinel.aspect.RateLimitAspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link FlowControlBehavior} 枚举测试。
 *
 * <p>验证每个枚举值通过 {@link RateLimitAspect} 内部映射后，
 * 对应正确的 Sentinel {@link RuleConstant} 控制行为常量。
 * 通过反射访问私有方法 {@code mapControlBehavior} 完成白盒测试。
 */
@DisplayName("FlowControlBehavior")
class FlowControlBehaviorTest {

    /**
     * 调用 {@link RateLimitAspect# mapControlBehavior} 私有方法，
     * 返回对应的 Sentinel RuleConstant 整数值。
     */
    private int mapBehavior(FlowControlBehavior behavior) throws Exception {
        RateLimitAspect aspect = new RateLimitAspect();
        Method method = RateLimitAspect.class.getDeclaredMethod("mapControlBehavior", FlowControlBehavior.class);
        method.setAccessible(true);
        return (int) method.invoke(aspect, behavior);
    }

    @Nested
    @DisplayName("shouldMapToCorrectSentinelConstants")
    class ShouldMapToCorrectSentinelConstants {

        @Test
        @DisplayName("FAST_FAIL 应映射到 CONTROL_BEHAVIOR_DEFAULT (0)")
        void fastFailMapsToBehaviorDefault() throws Exception {
            int actual = mapBehavior(FlowControlBehavior.FAST_FAIL);
            assertEquals(RuleConstant.CONTROL_BEHAVIOR_DEFAULT, actual,
                    "FAST_FAIL 应对应 Sentinel CONTROL_BEHAVIOR_DEFAULT");
        }

        @Test
        @DisplayName("WARM_UP 应映射到 CONTROL_BEHAVIOR_WARM_UP (1)")
        void warmUpMapsToBehaviorWarmUp() throws Exception {
            int actual = mapBehavior(FlowControlBehavior.WARM_UP);
            assertEquals(RuleConstant.CONTROL_BEHAVIOR_WARM_UP, actual,
                    "WARM_UP 应对应 Sentinel CONTROL_BEHAVIOR_WARM_UP");
        }

        @Test
        @DisplayName("RATE_LIMITER 应映射到 CONTROL_BEHAVIOR_RATE_LIMITER (2)")
        void rateLimiterMapsToBehaviorRateLimiter() throws Exception {
            int actual = mapBehavior(FlowControlBehavior.RATE_LIMITER);
            assertEquals(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, actual,
                    "RATE_LIMITER 应对应 Sentinel CONTROL_BEHAVIOR_RATE_LIMITER");
        }

        @Test
        @DisplayName("所有枚举值均有对应的非负 Sentinel 常量")
        void allEnumValuesMappedToNonNegativeConstants() throws Exception {
            for (FlowControlBehavior behavior : FlowControlBehavior.values()) {
                int constant = mapBehavior(behavior);
                assertEquals(true, constant >= 0,
                        "枚举值 " + behavior + " 的 Sentinel 常量应 >= 0，实际: " + constant);
            }
        }
    }
}

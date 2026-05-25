package com.eagle.common.observability;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.pressuretest.PressureTestContext;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Reactor Context 与 ThreadLocal/MDC 自动传播配置。
 *
 * <p>在 WebFlux 应用启动时调用 {@link Hooks#enableAutomaticContextPropagation()}，
 * 并向 {@link ContextRegistry} 注册公共的 {@link ThreadLocalAccessor}。
 *
 * <p>启用后：
 * <ul>
 *   <li>{@code MDC} 中的 requestId 等键会自动随 Reactor 链跨线程传递</li>
 *   <li>各 starter（tenant / idempotency 等）只需在自身 WebFlux 配置内追加注册自己的
 *       {@link ThreadLocalAccessor}，即可让 {@code ThreadLocal} 在响应式调用链上保持可见</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({Hooks.class, ContextRegistry.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ContextPropagationConfig {

    /**
     * MDC 中 requestId 在 Reactor Context / ContextRegistry 中的统一 key。
     * 业务代码可通过 {@code Mono.deferContextual(ctx -> ctx.get(MDC_REQUEST_ID_KEY))} 读取。
     */
    public static final String MDC_REQUEST_ID_KEY = ErrorResult.MDC_REQUEST_ID;

    /**
     * 压测标记在 Reactor Context / ContextRegistry 中的统一 key。
     */
    public static final String PRESSURE_TEST_KEY = "eagle.pressure-test";

    @PostConstruct
    public void initialize() {
        ContextRegistry registry = ContextRegistry.getInstance();
        registry.registerThreadLocalAccessor(new MdcRequestIdAccessor());
        registry.registerThreadLocalAccessor(new PressureTestAccessor());
        Hooks.enableAutomaticContextPropagation();
        log.info("Reactor automatic context propagation enabled "
                + "(MDC requestId + PressureTest flag bridged)");
    }

    /**
     * 把 {@code MDC[requestId]} 这一条 ThreadLocal 绑定到 Reactor Context。
     */
    private static final class MdcRequestIdAccessor implements ThreadLocalAccessor<String> {

        @Override
        public Object key() {
            return MDC_REQUEST_ID_KEY;
        }

        @Override
        public String getValue() {
            return MDC.get(MDC_REQUEST_ID_KEY);
        }

        @Override
        public void setValue(String value) {
            MDC.put(MDC_REQUEST_ID_KEY, value);
        }

        @Override
        public void setValue() {
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    /**
     * 把 {@link PressureTestContext} 的 ThreadLocal 绑定到 Reactor Context，
     * 让压测标记跨线程透传到下游业务 / 调度器。
     */
    private static final class PressureTestAccessor implements ThreadLocalAccessor<Boolean> {

        @Override
        public Object key() {
            return PRESSURE_TEST_KEY;
        }

        @Override
        public Boolean getValue() {
            return PressureTestContext.isPressureTest();
        }

        @Override
        public void setValue(Boolean value) {
            if (Boolean.TRUE.equals(value)) {
                PressureTestContext.mark();
            } else {
                PressureTestContext.clear();
            }
        }

        @Override
        public void setValue() {
            PressureTestContext.clear();
        }
    }
}

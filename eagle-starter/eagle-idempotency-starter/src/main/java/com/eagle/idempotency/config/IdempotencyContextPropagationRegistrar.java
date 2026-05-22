package com.eagle.idempotency.config;

import com.eagle.idempotency.support.ReactiveIdempotencyTokenContext;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import java.util.Map;

/**
 * 把 {@link ReactiveIdempotencyTokenContext} 的 ThreadLocal 注册到 {@link ContextRegistry}，
 * 让幂等 Token header 在 Reactor 链上跨线程透传到 AOP 切面。
 *
 * @author 孙士雄
 */
@Slf4j
@ConditionalOnClass({ContextRegistry.class, ThreadLocalAccessor.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class IdempotencyContextPropagationRegistrar {

    /**
     * 幂等 Token 上下文在 {@link ContextRegistry} 中的统一 key。
     */
    public static final String IDEMPOTENCY_CONTEXT_KEY = "eagle.idempotency.headers";

    @PostConstruct
    public void register() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new IdempotencyHeadersAccessor());
        log.info("Idempotency ThreadLocalAccessor registered: key={}", IDEMPOTENCY_CONTEXT_KEY);
    }

    private static final class IdempotencyHeadersAccessor implements ThreadLocalAccessor<Map<String, String>> {

        @Override
        public Object key() {
            return IDEMPOTENCY_CONTEXT_KEY;
        }

        @Override
        public Map<String, String> getValue() {
            return ReactiveIdempotencyTokenContext.snapshot();
        }

        @Override
        public void setValue(Map<String, String> value) {
            ReactiveIdempotencyTokenContext.restore(value);
        }

        @Override
        public void setValue() {
            ReactiveIdempotencyTokenContext.clear();
        }
    }
}

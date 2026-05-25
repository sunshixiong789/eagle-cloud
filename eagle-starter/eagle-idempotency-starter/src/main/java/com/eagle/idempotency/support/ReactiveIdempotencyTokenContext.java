package com.eagle.idempotency.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebFlux 环境下的幂等 Token 请求头容器。
 *
 * <p>由 {@link com.eagle.idempotency.filter.ReactiveIdempotencyTokenWebFilter} 在请求入口写入，
 * 由 {@link com.eagle.idempotency.support.ReactiveIdempotencyTokenResolver} 在 AOP 切面同步读取。
 *
 * <p>ThreadLocal 在 Reactor 链上跨线程的可见性由 {@code IdempotencyContextPropagationRegistrar}
 * 注册到 {@code io.micrometer.context.ContextRegistry} 的 {@code ThreadLocalAccessor} +
 * {@code Hooks.enableAutomaticContextPropagation()} 共同保证。
 *
 * @author eagle
 */
public final class ReactiveIdempotencyTokenContext {

    private static final ThreadLocal<Map<String, String>> CURRENT_HEADERS =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private ReactiveIdempotencyTokenContext() {
    }

    public static void set(String headerName, String token) {
        if (token != null) {
            CURRENT_HEADERS.get().put(headerName, token);
        }
    }

    public static void setAll(Map<String, String> headers) {
        CURRENT_HEADERS.get().putAll(headers);
    }

    public static String get(String headerName) {
        String value = CURRENT_HEADERS.get().get(headerName);
        return value != null ? value : CURRENT_HEADERS.get().get(headerName.toLowerCase());
    }

    /**
     * 内部接口：供 ThreadLocalAccessor 读出 / 写入完整 header map（实现快照与恢复）。
     */
    public static Map<String, String> snapshot() {
        return CURRENT_HEADERS.get();
    }

    public static void restore(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            clear();
            return;
        }
        CURRENT_HEADERS.set(new ConcurrentHashMap<>(headers));
    }

    public static void clear() {
        CURRENT_HEADERS.remove();
    }
}

package com.eagle.idempotency.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary request header context used during WebFlux handler invocation.
 *
 * @author 孙士雄
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

    public static void clear() {
        CURRENT_HEADERS.remove();
    }
}

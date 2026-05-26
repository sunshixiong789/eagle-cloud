package com.eagle.auth.core.infrastructure.security;

/**
 * 当前请求的 IP（由 SecurityFilter 写入，UserDetailsService 读取）
 *
 * @author sunshixiong
 */
public final class ClientIpHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private ClientIpHolder() {
    }

    public static void set(String ip) {
        HOLDER.set(ip);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

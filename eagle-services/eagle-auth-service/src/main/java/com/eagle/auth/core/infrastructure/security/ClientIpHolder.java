package com.eagle.auth.core.infrastructure.security;

/**
 * 当前请求的 IP（由 SecurityFilter 写入，UserDetailsService 读取）
 *
 * @author sunshixiong
 */
public final class ClientIpHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT_HOLDER = new ThreadLocal<>();

    private ClientIpHolder() {
    }

    public static void set(String ip) {
        HOLDER.set(ip);
    }

    public static void set(String ip, String userAgent) {
        HOLDER.set(ip);
        USER_AGENT_HOLDER.set(userAgent);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static String getUserAgent() {
        return USER_AGENT_HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
        USER_AGENT_HOLDER.remove();
    }
}

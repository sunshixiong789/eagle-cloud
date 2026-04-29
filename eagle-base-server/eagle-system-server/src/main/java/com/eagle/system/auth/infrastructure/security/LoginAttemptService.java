package com.eagle.system.auth.infrastructure.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录频率限制服务
 * <p>
 * 基于 IP 追踪登录失败次数，超过阈值后临时封锁该 IP，
 * 防止暴力破解攻击。
 *
 * @author sunshixiong
 */
@Service
public class LoginAttemptService {

    /** 同一 IP 在封锁窗口内允许的最大失败次数 */
    private static final int MAX_ATTEMPTS = 5;

    /** 封锁持续时长（毫秒），默认 30 分钟 */
    private static final long BLOCK_DURATION_MS = 30L * 60 * 1000;

    /** key: IP 地址，value: [失败次数, 过期时间戳] */
    private final ConcurrentHashMap<String, long[]> cache = new ConcurrentHashMap<>();

    /**
     * 记录一次登录失败
     *
     * @param ip 客户端 IP 地址
     */
    public void registerFailure(String ip) {
        cache.compute(ip, (key, current) -> {
            long now = System.currentTimeMillis();
            // 若记录不存在或已过期，重新计数
            if (current == null || now > current[1]) {
                return new long[]{1, now + BLOCK_DURATION_MS};
            }
            current[0]++;
            return current;
        });
    }

    /**
     * 登录成功后清除失败记录
     *
     * @param ip 客户端 IP 地址
     */
    public void registerSuccess(String ip) {
        cache.remove(ip);
    }

    /**
     * 判断该 IP 是否已被封锁
     *
     * <p>使用 {@code compute} 保证读-判-删三步原子执行，避免并发场景下的 TOCTOU 竞态。
     *
     * @param ip 客户端 IP 地址
     * @return true 表示已超过失败阈值，应拒绝请求
     */
    public boolean isBlocked(String ip) {
        long[] result = new long[]{0};
        cache.compute(ip, (key, entry) -> {
            if (entry == null) {
                return null;
            }
            // 已过期则原子清除，返回 null 触发移除
            if (System.currentTimeMillis() > entry[1]) {
                return null;
            }
            result[0] = entry[0];
            return entry;
        });
        return result[0] >= MAX_ATTEMPTS;
    }
}

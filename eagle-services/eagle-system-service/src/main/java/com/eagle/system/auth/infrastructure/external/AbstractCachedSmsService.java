package com.eagle.system.auth.infrastructure.external;

import com.eagle.common.util.LogMask;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.service.SmsService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 验证码型短信服务的共享骨架。
 *
 * <p>承载与服务商无关的通用逻辑：6 位验证码生成、内存缓存（5 分钟过期）、
 * 同号 60 秒频率限制、校验后失效。子类只需实现 {@link #doSend(String, String)}
 * 负责真正调用云服务商 API（阿里云 / 腾讯云 / 自建网关 …）。
 *
 * @author sunshixiong
 */
@Slf4j
public abstract class AbstractCachedSmsService implements SmsService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 验证码缓存：phone → code，5 分钟过期 */
    private final Cache<String, String> codeCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10000)
            .build();

    /** 发送频率限制缓存：phone → timestamp，60 秒过期 */
    private final Cache<String, Long> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(10000)
            .build();

    @Override
    public void sendCode(String phone) {
        // 原子检查：putIfAbsent 在并发场景下保证同一 phone 60 秒只能进入一次发送流程。
        // 之前 getIfPresent + put 两步存在并发窗口，多线程可能同时通过校验后重复发送。
        Long previous = rateLimitCache.asMap().putIfAbsent(phone, System.currentTimeMillis());
        if (previous != null) {
            throw AuthErrorCode.SMS_RATE_LIMIT.toServiceException();
        }

        String code = generateCode();
        codeCache.put(phone, code);

        if (isConfigured()) {
            doSend(phone, code);
        } else {
            // 未配置真实服务商凭据时，打印验证码到日志，便于本地/开发环境联调；
            // 即使是开发态也对手机号脱敏，code 仅在 isConfigured() == false 分支输出（生产不会进入）
            log.warn("{} 短信未配置，验证码: phone={}, code={}", providerName(), LogMask.phone(phone), code);
        }
        log.debug("sms send: provider={}, phone=[{}] (len={}), cacheSize={}, this={}",
                providerName(), LogMask.phone(phone), phone.length(),
                codeCache.estimatedSize(), System.identityHashCode(this));
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String cached = codeCache.getIfPresent(phone);
        log.debug("sms verify: provider={}, phone=[{}], cachedPresent={}",
                providerName(), LogMask.phone(phone), cached != null);
        if (cached != null && cached.equals(code)) {
            codeCache.invalidate(phone);
            return true;
        }
        return false;
    }

    /**
     * 服务商凭据是否已配置完整，false 时跳过真实发送只打印日志。
     */
    protected abstract boolean isConfigured();

    /**
     * 调用具体服务商发送验证码。
     *
     * @param phone 接收手机号
     * @param code  本次生成的 6 位验证码
     */
    protected abstract void doSend(String phone, String code);

    /**
     * 服务商名称（用于日志标识，如 {@code aliyun} / {@code tencent}）。
     */
    protected abstract String providerName();

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }
}

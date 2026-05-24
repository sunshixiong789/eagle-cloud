package com.eagle.auth.infrastructure.external;

import com.eagle.auth.domain.AuthErrorCode;
import com.eagle.auth.domain.service.PhoneOneClickService;
import com.eagle.auth.infrastructure.config.PhoneOneClickProperties;
import com.eagle.auth.infrastructure.external.provider.PhoneOneClickProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 手机号一键登录服务（路由层）
 * <p>
 * 根据 {@code eagle.auth.one-click.provider} 配置选择对应的 {@link PhoneOneClickProvider} 适配器，
 * 具体校验逻辑由各 Provider 自行实现：
 * <ul>
 *   <li>{@code mock}（默认）：access_token 直接当手机号校验，仅用于开发联调</li>
 *   <li>{@code aliyun}：阿里云号码认证（dypnsapi）</li>
 *   <li>{@code tencent}：腾讯云号码认证（PNSV，CommonClient）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
public class PhoneOneClickServiceImpl implements PhoneOneClickService {

    private final PhoneOneClickProperties properties;
    private final Map<String, PhoneOneClickProvider> providers;

    public PhoneOneClickServiceImpl(PhoneOneClickProperties properties,
                                    List<PhoneOneClickProvider> providers) {
        this.properties = properties;
        this.providers = providers.stream().collect(Collectors.toMap(
                p -> p.name().toLowerCase(Locale.ROOT),
                Function.identity()));
        log.info("一键登录已注册 Provider: {}, 当前生效: {}", this.providers.keySet(), properties.getProvider());
    }

    @Override
    public String verifyAndGetPhone(String accessToken) {
        if (!properties.isEnabled()) {
            throw AuthErrorCode.ONE_CLICK_PROVIDER_DISABLED.toServiceException();
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw AuthErrorCode.ONE_CLICK_TOKEN_REQUIRED.toDomainException();
        }
        String key = properties.getProvider() == null ? "" : properties.getProvider().toLowerCase(Locale.ROOT);
        PhoneOneClickProvider provider = providers.get(key);
        if (provider == null) {
            log.error("未找到一键登录 Provider: {}, 已注册={}", properties.getProvider(), providers.keySet());
            throw AuthErrorCode.ONE_CLICK_PROVIDER_DISABLED.toServiceException();
        }
        return provider.verifyAndGetPhone(accessToken);
    }
}

package com.eagle.system.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录频率限制开关配置
 *
 * <p>默认开启（生产环境保留暴力破解防护）；开发/调试环境可通过
 * {@code eagle.security.login-rate-limit.enabled=false} 关闭，避免反复联调时被自封 30 分钟。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.security.login-rate-limit")
public class LoginRateLimitProperties {

    /** 是否启用登录频率限制，默认 true */
    private boolean enabled = true;
}

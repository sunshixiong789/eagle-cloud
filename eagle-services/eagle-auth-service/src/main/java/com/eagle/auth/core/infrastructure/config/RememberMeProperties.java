package com.eagle.auth.core.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 表单登录"记住我"配置（hash-token 模式）。
 *
 * <p>对应 application.yml 中 {@code eagle.remember-me} 前缀。表单登录页勾选"记住我"后，
 * Spring Security 的 {@code TokenBasedRememberMeServices} 签发一个自包含的持久化 Cookie，
 * 其值为 {@code base64(username + ':' + expiryTime + ':' + algo + ':' + hash)}，其中
 * hash 由 {@code username + expiryTime + password + key} 计算。会话过期或关闭浏览器后，
 * {@code RememberMeAuthenticationFilter} 凭该 Cookie 自动重建认证态。
 *
 * <p><strong>{@link #key} 必须稳定且集群一致</strong>：未显式配置时 Spring 会在启动时随机生成
 * 一个 key，导致重启 / 多实例之间签发的 Cookie 互相无法校验，"记住我"静默失效——这正是本字段
 * 设为 {@code @NotBlank} fail-fast、强制由环境变量注入的原因。
 *
 * @author eagle
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "eagle.remember-me")
public class RememberMeProperties {

    /**
     * remember-me Cookie 签名密钥。
     *
     * <p>缺失即 fail-fast；生产必须通过环境变量 {@code EAGLE_REMEMBER_ME_KEY} 注入足够随机的强密钥。
     * 集群所有实例必须配置完全相同的值。
     */
    @NotBlank(message = "remember-me 签名密钥不能为空，请设置环境变量 EAGLE_REMEMBER_ME_KEY")
    private String key;

    /**
     * remember-me Cookie 有效期，默认 14 天。
     */
    @NotNull
    private Duration validity = Duration.ofDays(14);
}

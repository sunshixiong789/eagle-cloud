package com.eagle.auth.core.infrastructure.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Apple Sign In token 验证配置。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "eagle.apple-authentication")
public class AppleAuthenticationProperties {

    /** 未配置 Apple 服务端凭据时保持关闭，避免误以为登录链路可用。 */
    private boolean enabled;

    @NotBlank
    private String issuer = "https://appleid.apple.com";

    @NotBlank
    private String jwkSetUri = "https://appleid.apple.com/auth/keys";

    @NotBlank
    private String tokenUri = "https://appleid.apple.com/auth/token";

    @NotBlank
    private String revokeUri = "https://appleid.apple.com/auth/revoke";

    /** iOS App 的 bundle identifier，也是 Apple identity token 的 audience。 */
    @NotBlank
    private String clientId = "com.shengxinfast.app";

    /** Apple Developer Membership 的 Team ID。 */
    private String teamId;

    /** Sign in with Apple 私钥的 Key ID。 */
    private String keyId;

    /** Apple 下载的 .p8 私钥 PEM；只允许通过 Secret / 环境变量注入。 */
    private String privateKey;

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(10);

    @AssertTrue(message = "Apple 登录启用时必须配置 team-id、key-id 和 private-key")
    public boolean isServerCredentialComplete() {
        return !enabled || (hasText(teamId) && hasText(keyId) && hasText(privateKey));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.eagle.auth.core.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Apple Sign In token 验证配置。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "eagle.apple-authentication")
public class AppleAuthenticationProperties {

    @NotBlank
    private String issuer = "https://appleid.apple.com";

    @NotBlank
    private String jwkSetUri = "https://appleid.apple.com/auth/keys";

    /** iOS App 的 bundle identifier，也是 Apple identity token 的 audience。 */
    @NotBlank
    private String clientId = "com.shengxinfast.app";
}

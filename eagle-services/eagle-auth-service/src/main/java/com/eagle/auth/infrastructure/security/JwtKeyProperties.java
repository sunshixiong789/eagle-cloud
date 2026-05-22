package com.eagle.auth.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT 签名密钥配置（支持多 key 滚动）。
 *
 * <p>从 PKCS12 密钥库文件加载 RSA 密钥对。{@link #keyAlias} 是当前 active key（用于签名），
 * {@link #previousKeyAliases} 是历史 key 列表（仅用于校验未过期的存量 token），全部 key
 * 都会进入 JWKSet 暴露给资源服务器，每个 key 用 alias 作为 kid。
 *
 * <p>密钥轮转流程：
 * <ol>
 *   <li>{@code keytool -genkeypair -alias eagle-jwt-202607 ...} 新生成一对密钥</li>
 *   <li>把当前 {@code key-alias} 移到 {@code previous-key-aliases}</li>
 *   <li>{@code key-alias} 指向新 alias，重启服务 → 新签发 token 使用新 key，旧 token 仍可校验</li>
 *   <li>所有旧 token TTL 过期后（默认 30 天），从 {@code previous-key-aliases} 移除旧 alias</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "eagle.jwt")
public class JwtKeyProperties {

    /**
     * 密钥库文件路径。
     * <p>支持 {@code classpath:jwt-keystore.p12} 或 {@code file:/etc/eagle/jwt-keystore.p12}</p>
     */
    @NotNull(message = "JWT 密钥库路径不能为空，请配置 eagle.jwt.keystore-location")
    private Resource keystoreLocation;

    /**
     * 密钥库密码。
     */
    @NotBlank(message = "JWT 密钥库密码不能为空，请设置环境变量 EAGLE_JWT_KEYSTORE_PASSWORD")
    private String keystorePassword;

    /**
     * 当前 active 密钥别名（用于签名）。
     */
    private String keyAlias = "eagle-jwt";

    /**
     * 历史密钥别名列表（仅用于校验存量 token，不参与签名）。
     */
    private List<String> previousKeyAliases = new ArrayList<>();
}

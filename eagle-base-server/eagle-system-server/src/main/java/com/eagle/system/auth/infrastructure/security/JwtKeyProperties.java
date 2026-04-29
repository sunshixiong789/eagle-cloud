package com.eagle.system.auth.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 签名密钥配置
 *
 * <p>从 PKCS12 密钥库文件加载 RSA 密钥对，用于 OAuth2 Authorization Server 的 JWT 签发与验证。
 * 密钥持久化后，服务重启不会导致已签发的 Token 失效。</p>
 *
 * <p>生成密钥库：</p>
 * <pre>
 * keytool -genkeypair -alias eagle-jwt -keyalg RSA -keysize 2048 \
 *   -storetype PKCS12 -keystore jwt-keystore.p12 \
 *   -storepass ${EAGLE_JWT_KEYSTORE_PASSWORD} -validity 3650
 * </pre>
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
     * 密钥库文件路径
     * <p>支持 {@code classpath:jwt-keystore.p12} 或 {@code file:/etc/eagle/jwt-keystore.p12}</p>
     */
    @NotNull(message = "JWT 密钥库路径不能为空，请配置 eagle.jwt.keystore-location")
    private Resource keystoreLocation;

    /** 密钥库密码 */
    @NotBlank(message = "JWT 密钥库密码不能为空，请设置环境变量 EAGLE_JWT_KEYSTORE_PASSWORD")
    private String keystorePassword;

    /** 密钥别名 */
    private String keyAlias = "eagle-jwt";
}
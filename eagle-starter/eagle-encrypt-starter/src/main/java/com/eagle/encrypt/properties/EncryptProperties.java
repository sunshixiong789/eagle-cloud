package com.eagle.encrypt.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 字段级加密配置属性。
 *
 * <p>密钥必须通过环境变量注入，<strong>禁止写入配置文件或代码</strong>：
 * <pre>
 * # 在容器/K8s Secret 中注入
 * EAGLE_ENCRYPT_SECRET_KEY=your-32-chars-secret-key
 * </pre>
 *
 * <p>application.yml 示例：
 * <pre>
 * eagle:
 *   encrypt:
 *     secret-key: ${EAGLE_ENCRYPT_SECRET_KEY}
 * </pre>
 *
 * <p>在 JPA 实体字段上使用 {@link jakarta.persistence.Convert} 注解即可自动加解密：
 * <pre>
 * &#64;Convert(converter = EncryptedStringConverter.class)
 * &#64;Column(name = "mobile")
 * private String mobile;
 * </pre>
 *
 * <p>未配置 {@code secret-key} 时转换器以透明模式（不加密）运行。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.encrypt")
public class EncryptProperties {

    /**
     * AES-256 加密密钥，通过 SHA-256 哈希后得到 32 字节密钥。
     * 必须通过环境变量 {@code EAGLE_ENCRYPT_SECRET_KEY} 注入，禁止硬编码。
     */
    private String secretKey;
}

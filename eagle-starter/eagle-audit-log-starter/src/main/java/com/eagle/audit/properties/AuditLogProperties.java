package com.eagle.audit.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计日志配置属性。
 *
 * <p>application.yml 示例：
 * <pre>
 * eagle:
 *   audit-log:
 *     enabled: true
 *     max-args-length: 2000
 *     max-result-length: 2000
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.audit-log")
public class AuditLogProperties {

    /**
     * 是否启用审计日志。
     */
    private boolean enabled = true;

    /**
     * 请求参数序列化后的最大字符数，超出截断。
     */
    private int maxArgsLength = 2000;

    /**
     * 返回结果序列化后的最大字符数，超出截断。
     */
    private int maxResultLength = 2000;
}

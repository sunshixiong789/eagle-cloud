package com.eagle.audit.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计日志配置属性。
 *
 * <p>application.yml 示例:
 * <pre>
 * eagle:
 *   audit-log:
 *     controller-enabled: true      # 暴露 /audit-logs 查询接口
 *     permit-role: admin            # 查询接口所需角色
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
     * 请求参数序列化后的最大字符数,超出截断。
     */
    private int maxArgsLength = 2000;

    /**
     * 返回结果序列化后的最大字符数,超出截断。
     */
    private int maxResultLength = 2000;

    /**
     * 是否暴露 {@code /audit-logs} 查询接口(默认关闭,管理后台服务显式开启)。
     */
    private boolean controllerEnabled = false;

    /**
     * 查询接口所需角色名(无需 {@code ROLE_} 前缀)。
     */
    private String permitRole = "admin";
}

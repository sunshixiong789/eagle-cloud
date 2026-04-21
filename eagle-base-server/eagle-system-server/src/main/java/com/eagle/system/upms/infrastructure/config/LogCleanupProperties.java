package com.eagle.system.upms.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 审计日志清理配置属性
 * <p>
 * 对应 application.yml 中的 {@code eagle.log.cleanup} 前缀配置。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.log.cleanup")
public class LogCleanupProperties {

    /** 是否启用自动清理 */
    private boolean enabled = true;

    /** 日志保留天数（超过此天数的日志将被删除） */
    private int retentionDays = 90;

    /** cron 表达式（默认每天凌晨 2 点执行） */
    private String cron = "0 0 2 * * ?";
}

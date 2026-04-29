package com.eagle.mybatis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis-Plus 配置属性。
 *
 * <p>通过 {@code eagle.mybatis.*} 前缀绑定，统一管理 MyBatis-Plus 插件及行为开关。
 * 所有属性均提供合理的默认值，消费方可按需覆盖。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.mybatis")
public class MybatisProperties {

    /** 是否启用乐观锁插件 */
    private boolean optimisticLockerEnabled = true;

    /** 是否启用 SQL 性能分析（仅开发环境） */
    private boolean performanceEnabled = false;

    /** 慢 SQL 阈值（ms），超过此值打印 WARN 日志 */
    private long slowSqlMillis = 1000;
}
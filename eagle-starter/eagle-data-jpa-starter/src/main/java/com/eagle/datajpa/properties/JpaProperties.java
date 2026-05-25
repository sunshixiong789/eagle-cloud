package com.eagle.datajpa.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JPA / Hibernate 扩展配置属性。
 *
 * <p>提供对 Hibernate 批量写入、慢 SQL 阈值等常用参数的类型安全配置，
 * 统一收敛各服务 {@code application.yml} 中散落的 {@code spring.jpa.properties.*} 设置。
 *
 * <p>示例（application.yml）：
 * <pre>
 * eagle:
 *   jpa:
 *     batch-size: 100
 *     slow-query-threshold-millis: 2000
 *     show-sql: false
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.jpa")
public class JpaProperties {

    /**
     * Hibernate JDBC 批量写入大小。
     * 启用后 saveAll() 将合并为批量 INSERT/UPDATE，显著提升批量操作性能。
     * 设为 0 表示禁用批量写入。
     */
    private int batchSize = 100;

    /**
     * 是否对批量 INSERT 重新排序，使相同类型的 INSERT 相邻，提升批处理效率。
     */
    private boolean orderInserts = true;

    /**
     * 是否对批量 UPDATE 重新排序。
     */
    private boolean orderUpdates = true;

    /**
     * 慢 SQL 阈值（毫秒）。超过此阈值的查询会以 WARN 级别输出日志。
     * 设为 0 表示禁用慢 SQL 检测。
     */
    private long slowQueryThresholdMillis = 2000;

    /**
     * 是否在日志中输出 SQL 语句（等同于 spring.jpa.show-sql，此处作为统一配置入口）。
     * 生产环境建议设为 false。
     */
    private boolean showSql = false;
}

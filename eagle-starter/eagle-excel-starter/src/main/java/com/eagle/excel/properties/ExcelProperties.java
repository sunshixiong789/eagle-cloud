package com.eagle.excel.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel 导入导出配置属性。
 *
 * <p>示例（application.yml）：
 * <pre>
 * eagle:
 *   excel:
 *     max-rows: 50000
 *     streaming-threshold: 5000
 *     date-format: yyyy-MM-dd
 *     datetime-format: yyyy-MM-dd HH:mm:ss
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.excel")
public class ExcelProperties {

    /**
     * 单次导入最大行数（不含表头），超过此值抛出异常防止内存溢出。
     */
    private int maxRows = 100_000;

    /**
     * 流式写入阈值：数据行数超过此值时自动切换为 SXSSF 流式模式，降低内存占用。
     * SXSSF 不支持读取已写入的行，仅适用于顺序写场景。
     */
    private int streamingThreshold = 10_000;

    /**
     * 全局日期格式（{@code LocalDate} 字段）。
     */
    private String dateFormat = "yyyy-MM-dd";

    /**
     * 全局日期时间格式（{@code LocalDateTime} 字段）。
     */
    private String datetimeFormat = "yyyy-MM-dd HH:mm:ss";

    /**
     * 导出文件默认 Sheet 名称。
     */
    private String defaultSheetName = "Sheet1";
}

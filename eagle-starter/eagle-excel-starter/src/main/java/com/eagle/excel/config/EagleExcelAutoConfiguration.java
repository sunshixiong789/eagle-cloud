package com.eagle.excel.config;

import com.eagle.excel.properties.ExcelProperties;
import com.eagle.excel.reader.ExcelReader;
import com.eagle.excel.writer.ExcelWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Excel 导入导出自动配置。
 *
 * <p>注册 {@link ExcelReader} 和 {@link ExcelWriter} Bean，
 * 由 {@link ExcelProperties}（{@code eagle.excel.*}）控制行为。
 *
 * @author eagle
 */
@AutoConfiguration
@ConditionalOnClass(Workbook.class)
@ConditionalOnProperty(name = "eagle.excel.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ExcelProperties.class)
public class EagleExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelReader excelReader(ExcelProperties properties) {
        return new ExcelReader(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelWriter excelWriter(ExcelProperties properties) {
        return new ExcelWriter(properties);
    }
}

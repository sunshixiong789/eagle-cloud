package com.eagle.datasource.properties;

/**
 * @deprecated 请使用 {@link DynamicDataSourceProperties}。此类与 Spring Boot 内置的
 * {@code org.springframework.boot.autoconfigure.jdbc.DataSourceProperties} 同名，
 * 极易混淆，将在后续版本删除。
 */
@Deprecated(since = "2.0", forRemoval = true)
public class DataSourceProperties extends DynamicDataSourceProperties {
}

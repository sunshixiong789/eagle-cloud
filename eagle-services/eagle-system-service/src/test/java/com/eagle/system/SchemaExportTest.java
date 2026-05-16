package com.eagle.system;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 一次性 Hibernate 元数据导出工具。
 *
 * <p>用于生成 Flyway baseline 脚本：JPA EntityManagerFactory 启动时根据 {@code @Entity}
 * 元数据写出 {@code build/schema-export.sql}，整理后落入 {@code db/migration/V*__init_business_tables.sql}。</p>
 *
 * <p>默认 {@code @Disabled}，仅在需要重新生成时手动启用：去掉 {@code @Disabled} 跑一次：</p>
 * <pre>./gradlew :eagle-services:eagle-system-service:test --tests "*.SchemaExportTest"</pre>
 *
 * @author sunshixiong
 */
@Disabled("only run manually when regenerating db/migration baseline")
@ActiveProfiles("local")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/schema-export.sql",
        "spring.jpa.properties.jakarta.persistence.schema-generation.create-source=metadata",
        "spring.flyway.enabled=false"
})
class SchemaExportTest {

    @Test
    void exportSchema() {
        // Schema export happens during EntityManagerFactory bootstrap;
        // 这个方法本身不需要逻辑，目的只是让 Spring 上下文加载完成。
    }
}

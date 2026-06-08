package com.eagle.system;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 一次性 Hibernate 元数据导出工具。
 *
 * <p>用于生成生产环境首次部署的 DDL 脚本:JPA EntityManagerFactory 启动时根据
 * {@code @Entity} 元数据写出 {@code build/schema-export.sql},运维审核后手工应用到 PostgreSQL。
 * 后续统一引入 Flyway 时,该脚本将作为 baseline 落入 {@code db/migration/V*__init_business_tables.sql}。</p>
 *
 * <p>默认 {@code @Disabled},仅在需要重新生成时手动启用:去掉 {@code @Disabled} 跑一次:</p>
 * <pre>gradle :eagle-services:eagle-system-service:test --tests "*.SchemaExportTest"</pre>
 *
 * @author sunshixiong
 */
@Disabled("only run manually when regenerating schema-export.sql")
@ActiveProfiles("local")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/schema-export.sql",
        "spring.jpa.properties.jakarta.persistence.schema-generation.create-source=metadata"
})
class SchemaExportTest {

    @Test
    @DisplayName("应能导出数据库 Schema")
    void exportSchema() {
        // Schema export happens during EntityManagerFactory bootstrap;
        // 这个方法本身不需要逻辑，目的只是让 Spring 上下文加载完成。
    }
}

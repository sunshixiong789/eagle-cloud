package com.eagle.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * profile 配置文件的完整性回归（system-service）。
 *
 * <p>基线 {@code ddl-auto} 取的是生产安全的 {@code validate}，local / dev 必须覆盖成
 * {@code update}——否则空库启动时 Hibernate 不建表，直接校验失败起不来。
 * 这条差异只有真跑一次才会暴露，故在此锁住。
 *
 * <p>这里只解析配置、不建任何 bean，因此不需要数据库 / Redis / Consul 在跑。
 *
 * <p><strong>断言范围限于 profile 文件自身声明的键</strong>：{@code src/test/resources/application.yml}
 * 在测试 classpath 上遮蔽了 {@code src/main} 的同名基线文件（同名资源只取第一个），
 * 因此基线里那些 {@code ${VAR:default}} 占位符（如 {@code spring.cloud.consul.config.enabled}）
 * 在测试里并不存在，断言它们只会测到遮蔽产物而非真实配置。
 *
 * @author sunshixiong
 */
@DisplayName("profile 配置文件")
class ProfileConfigurationTest {

    private void withProfile(String profile, Consumer<Environment> assertions) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of())
                .withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertions.accept(((AssertableApplicationContext) context).getEnvironment());
                });
    }

    @Nested
    @DisplayName("local")
    class Local {

        @Test
        @DisplayName("ddl-auto=update：空库首次启动需由 Hibernate 建表")
        void overridesDdlAutoToUpdate() {
            withProfile("local", env ->
                    assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto"))
                            .as("基线是 validate，local 不覆盖成 update 则空库起不来")
                            .isEqualTo("update"));
        }

        @Test
        @DisplayName("关闭 Consul 服务发现与配置中心")
        void disablesConsul() {
            withProfile("local", env -> {
                assertThat(env.getProperty("spring.cloud.consul.discovery.enabled")).isEqualTo("false");
                assertThat(env.getProperty("spring.cloud.consul.config.enabled")).isEqualTo("false");
            });
        }

        @Test
        @DisplayName("凭据默认值对齐本地 compose，开箱即跑")
        void usesLocalDefaults() {
            withProfile("local", env -> {
                assertThat(env.getProperty("spring.datasource.username")).isEqualTo("eagle");
                assertThat(env.getProperty("spring.data.redis.password")).isEqualTo("redis123456");
            });
        }
    }

    @Nested
    @DisplayName("dev")
    class Dev {

        @Test
        @DisplayName("ddl-auto=update：容器化开发环境同样靠 Hibernate 同步 schema")
        void updatesSchema() {
            withProfile("dev", env ->
                    assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update"));
        }

        @Test
        @DisplayName("暴露调试用 Actuator 端点")
        void exposesDebugEndpoints() {
            withProfile("dev", env ->
                    assertThat(env.getProperty("management.endpoints.web.exposure.include"))
                            .contains("loggers", "env", "mappings"));
        }
    }

    @Nested
    @DisplayName("prod")
    class Prod {

        @Test
        @DisplayName("ddl-auto=validate：生产禁止运行时改表")
        void locksSchemaToValidate() {
            withProfile("prod", env ->
                    assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate"));
        }

        @Test
        @DisplayName("Swagger 关闭且 Actuator 收窄")
        void hardensExposure() {
            withProfile("prod", env -> {
                assertThat(env.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
                assertThat(env.getProperty("management.endpoints.web.exposure.include"))
                        .isEqualTo("health,info,prometheus");
            });
        }
    }
}

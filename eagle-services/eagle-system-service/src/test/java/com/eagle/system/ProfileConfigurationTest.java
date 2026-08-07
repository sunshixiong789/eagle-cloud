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

    /**
     * 一并关掉 Consul 配置中心：local profile 把它指向了开发环境的真实地址
     * （118.24.138.189:8500），不关的话每跑一次测试都要走一趟外网，还会因 ACL 返回 403。
     */
    private void withProfile(String profile, Consumer<Environment> assertions) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of())
                .withPropertyValues(
                        "spring.profiles.active=" + profile,
                        "spring.cloud.consul.config.enabled=false")
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
        @DisplayName("关闭服务发现：本机 IP 在 dev 集群内不可达，注册上去只会制造死实例")
        void disablesDiscovery() {
            withProfile("local", env ->
                    assertThat(env.getProperty("spring.cloud.consul.discovery.enabled"))
                            .isEqualTo("false"));
        }

        @Test
        @DisplayName("中间件地址覆盖为开发环境公网地址，绕开 KV 里的容器名")
        void overridesAddressesToDevHost() {
            withProfile("local", env -> {
                assertThat(env.getProperty("spring.datasource.url"))
                        .isEqualTo("jdbc:postgresql://118.24.138.189:5432/eagle_system");
                assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("118.24.138.189");
                assertThat(env.getProperty("spring.rabbitmq.host")).isEqualTo("118.24.138.189");
            });
        }

        @Test
        @DisplayName("消费组带 _local 后缀，不与 dev 的 system 抢同一条队列")
        void isolatesAmqpConsumerGroup() {
            withProfile("local", env ->
                    assertThat(env.getProperty("eagle.amqp.consumer-group"))
                            .isEqualTo("system_consumer_local")
                            .isNotEqualTo("system_consumer"));
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

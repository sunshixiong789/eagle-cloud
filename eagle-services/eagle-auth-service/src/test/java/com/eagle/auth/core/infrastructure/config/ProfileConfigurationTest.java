package com.eagle.auth.core.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.env.Environment;
import org.springframework.util.PlaceholderResolutionException;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * profile 配置文件的完整性回归。
 *
 * <p>盯住一类只在启动时才暴露的问题：基线 application.yml 里那些**故意不给默认值**的占位符
 * （缺失即 fail-fast），在 dev/prod 下由 Consul KV 填上，但 local **不读 KV**——
 * 一旦有人往基线新增一个无默认值的 {@code ${VAR}} 而忘了在 application-local.yml 补默认值，
 * 本地就会直接 {@code Could not resolve placeholder} 起不来，且只有真正跑一次才会发现。
 *
 * <p>这里只解析配置、不建任何 bean，因此不需要数据库 / Redis / Consul 在跑。
 *
 * @author sunshixiong
 */
@DisplayName("profile 配置文件")
class ProfileConfigurationTest {

    /**
     * 只跑配置解析：不注册自动配置，避免连数据库 / Redis。
     *
     * <p>一并关掉 Consul 配置中心：local profile 把它指向了开发环境的真实地址
     * （118.24.138.189:8500），不关的话每跑一次测试都要走一趟外网，还会因 ACL 返回 403。
     * 代价是 {@code consul.config.enabled} 自身的取值无法在此断言——要断言它就必须真连。
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
        @DisplayName("中间件地址覆盖为开发环境公网地址，绕开 KV 里的容器名")
        void overridesAddressesToDevHost() {
            withProfile("local", env -> {
                // KV 里 DB_HOST=postgres / REDIS_HOST=redis / RABBITMQ_HOST=rabbitmq 都是容器名，
                // 只在 dev 的 compose 网络内可解析；覆盖终值属性才能压过它们。
                assertThat(env.getProperty("spring.datasource.url"))
                        .isEqualTo("jdbc:postgresql://118.24.138.189:5432/eagle_auth");
                assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("118.24.138.189");
                assertThat(env.getProperty("spring.rabbitmq.host")).isEqualTo("118.24.138.189");
            });
        }

        @Test
        @DisplayName("地址覆盖用 LOCAL_* 变量名，避免被 KV 的同名键顶掉")
        void addressOverrideUsesDistinctVariableNames() {
            // 若写成 ${DB_HOST:118.24.138.189}，KV 提供的 DB_HOST=postgres 会赢，覆盖失效。
            // 这里验证 LOCAL_DB_HOST 确实是生效的那个入口。
            new ApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer())
                    .withConfiguration(AutoConfigurations.of())
                    .withPropertyValues(
                            "spring.profiles.active=local",
                            "spring.cloud.consul.config.enabled=false",
                            "LOCAL_DB_HOST=localhost",
                            "DB_HOST=postgres")
                    .run(context -> assertThat(((AssertableApplicationContext) context)
                            .getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://localhost:5432/eagle_auth"));
        }

        @Test
        @DisplayName("关闭服务发现：本机 IP 在 dev 集群内不可达，注册上去只会制造死实例")
        void disablesDiscovery() {
            withProfile("local", env ->
                    assertThat(env.getProperty("spring.cloud.consul.discovery.enabled"))
                            .isEqualTo("false"));
        }

        @Test
        @DisplayName("issuer 指向本机，不沿用 KV 里的 dev 域名")
        void usesLocalIssuer() {
            withProfile("local", env ->
                    assertThat(env.getProperty("eagle.oauth.issuer"))
                            .isEqualTo("http://localhost:9090"));
        }

        @Test
        @DisplayName("消费组带 _local 后缀，不与 dev 的 auth 抢同一条队列")
        void isolatesAmqpConsumerGroup() {
            withProfile("local", env ->
                    assertThat(env.getProperty("eagle.amqp.consumer-group"))
                            .isEqualTo("auth_consumer_local")
                            .isNotEqualTo("auth_consumer"));
        }

        @Test
        @DisplayName("不给机密兜底默认值：没配 CONSUL_TOKEN 时启动期就明确报缺")
        void keepsSecretsFailFast() {
            // 连的是 dev 的真库，宁可 fail-fast 报缺 EAGLE_ADMIN_PASSWORD，
            // 也好过用一个写死的错值连上去。
            withProfile("local", env -> {
                assertThatThrownBy(() -> env.getProperty("eagle.admin.password"))
                        .isInstanceOf(PlaceholderResolutionException.class);
                assertThatThrownBy(() -> env.getProperty("eagle.remember-me.key"))
                        .isInstanceOf(PlaceholderResolutionException.class);
            });
        }
    }

    @Nested
    @DisplayName("dev")
    class Dev {

        @Test
        @DisplayName("保持服务注册开启：容器内需要注册进 Consul 供网关路由")
        void keepsDiscoveryEnabled() {
            // config.enabled 不在此断言 —— withProfile 为避免测试走外网把它强制关掉了。
            withProfile("dev", env ->
                    assertThat(env.getProperty("spring.cloud.consul.discovery.enabled"))
                            .isEqualTo("true"));
        }

        @Test
        @DisplayName("不覆盖任何连接地址：dev 跑在 compose 网络内，容器名可直接解析")
        void keepsKvSuppliedAddresses() {
            // 与 local 相反：dev 必须让 KV 的 DB_HOST=postgres 等原样生效，
            // profile 文件一旦写死地址，KV 就再也改不动了。
            // 无 KV 时占位符回落到基线默认值；关键是不能出现 local 那个写死的公网地址。
            withProfile("dev", env -> {
                assertThat(env.getProperty("spring.datasource.url"))
                        .isEqualTo("jdbc:postgresql://localhost:5432/eagle_auth")
                        .doesNotContain("118.24.138.189");
                assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("127.0.0.1");
            });
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
        @DisplayName("Swagger 与 api-docs 强制关闭")
        void disablesSwagger() {
            withProfile("prod", env -> {
                assertThat(env.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
                assertThat(env.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
            });
        }

        @Test
        @DisplayName("Actuator 收窄到 health/info/prometheus，不暴露 env/mappings")
        void narrowsActuator() {
            withProfile("prod", env -> {
                String include = env.getProperty("management.endpoints.web.exposure.include");
                assertThat(include).isEqualTo("health,info,prometheus");
                assertThat(include).doesNotContain("env", "mappings", "beans");
            });
        }

        @Test
        @DisplayName("不给任何必填机密兜底默认值，缺失即 fail-fast")
        void keepsSecretsFailFast() {
            // prod 下这些值只能来自 Consul KV / 环境变量；profile 文件里写死默认值会让
            // 「忘配置」变成「用弱默认值静默上线」，这里守住不回退。
            // 未注入时占位符无法解析，取值即抛 —— 这正是期望的 fail-fast 行为。
            withProfile("prod", env -> {
                assertThatThrownBy(() -> env.getProperty("eagle.admin.password"))
                        .as("prod 的 admin 密码不该有兜底默认值")
                        .isInstanceOf(PlaceholderResolutionException.class);
                assertThatThrownBy(() -> env.getProperty("eagle.remember-me.key"))
                        .as("prod 的 remember-me key 不该有兜底默认值")
                        .isInstanceOf(PlaceholderResolutionException.class);
            });
        }
    }
}

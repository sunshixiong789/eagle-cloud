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
     * 基线里故意无默认值的必填项，local 必须逐一补上默认值。
     * 新增同类占位符时请同步扩充本清单。
     */
    private static final String[] REQUIRED_KEYS = {
            "eagle.admin.password",
            "eagle.jwt.keystore-password",
            "eagle.remember-me.key",
            "eagle.oauth.issuer",
    };

    /**
     * 只跑配置解析：不注册自动配置，避免连数据库 / Redis。
     */
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
        @DisplayName("基线所有无默认值的必填项都已补上默认值，本地可直接启动")
        void resolvesEveryRequiredPlaceholder() {
            withProfile("local", env -> {
                for (String key : REQUIRED_KEYS) {
                    assertThat(env.getProperty(key))
                            .as("local 缺少必填项默认值：%s —— 本地启动会 Could not resolve placeholder", key)
                            .isNotBlank();
                }
            });
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
        @DisplayName("issuer 指向本地端口，凭据默认值对齐本地 compose")
        void usesLocalDefaults() {
            withProfile("local", env -> {
                assertThat(env.getProperty("eagle.oauth.issuer")).isEqualTo("http://localhost:9090");
                assertThat(env.getProperty("spring.datasource.username")).isEqualTo("eagle");
                assertThat(env.getProperty("spring.data.redis.password")).isEqualTo("redis123456");
            });
        }

        @Test
        @DisplayName("keystore 密码与仓库内 jwt-keystore.p12 的实际密码一致")
        void keystorePasswordMatchesBundledKeystore() {
            withProfile("local", env ->
                    assertThat(env.getProperty("eagle.jwt.keystore-password"))
                            .as("改这个值会导致本地打不开 classpath:jwt-keystore.p12")
                            .isEqualTo("eagle-jwt-dev-2026"));
        }
    }

    @Nested
    @DisplayName("dev")
    class Dev {

        @Test
        @DisplayName("保持 Consul 开启：连接信息与凭据来自 KV，不写死在 profile 里")
        void keepsConsulEnabled() {
            withProfile("dev", env -> {
                assertThat(env.getProperty("spring.cloud.consul.discovery.enabled")).isEqualTo("true");
                assertThat(env.getProperty("spring.cloud.consul.config.enabled")).isEqualTo("true");
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

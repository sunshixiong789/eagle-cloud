package com.eagle.amqp;

import com.eagle.amqp.config.AmqpListenerRegistrar;
import com.eagle.amqp.config.EagleAmqpAutoConfiguration;
import com.eagle.amqp.config.EagleAmqpDefaultsEnvironmentPostProcessor;
import com.eagle.amqp.publisher.DomainEventPublisher;
import com.eagle.amqp.support.EagleRepublishRecoverer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * starter 的装配契约。
 *
 * <p><b>这组测试守的是一条踩过的坑</b>：容器一旦自己 {@code new} 出来，
 * {@code spring.rabbitmq.listener.*} 整片配置会静默失效 —— 使用方照官方文档配，
 * 行为纹丝不动且没有任何报错。现在容器由 Boot 的
 * {@code SimpleRabbitListenerContainerFactoryConfigurer} 装配，
 * 下面逐条钉住「标准配置键确实被读到了」。
 *
 * <p>不连真实 broker：{@code ApplicationContextRunner} 只做装配，不建立连接。
 */
@DisplayName("AMQP starter 自动装配")
class AmqpAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withConfiguration(AutoConfigurations.of(
                    RabbitAutoConfiguration.class,
                    EagleAmqpAutoConfiguration.class));

    @Nested
    @DisplayName("核心 bean")
    class CoreBeans {

        @Test
        @DisplayName("引入 starter 即装配，不需要任何 eagle.amqp.enabled 开关")
        void shouldRegisterCoreBeansWithoutAnySwitch() {
            runner.run(context -> assertThat(context)
                    .hasSingleBean(DomainEventPublisher.class)
                    .hasSingleBean(AmqpListenerRegistrar.class)
                    .hasSingleBean(MessageRecoverer.class)
                    .hasSingleBean(RabbitListenerRetrySettingsCustomizer.class));
        }

        @Test
        @DisplayName("recoverer 声明成 bean，Boot 才会把它装进 retry advice")
        void shouldExposeRecovererAsBeanSoBootCanWireIt() {
            runner.run(context -> assertThat(context.getBean(MessageRecoverer.class))
                    .isInstanceOf(EagleRepublishRecoverer.class));
        }

        @Test
        @DisplayName("主工厂与 DLQ 工厂分开：DLQ 不能挂 retry，否则死信在 DLQ 里打转")
        void shouldRegisterSeparateFactoriesForMainAndDlq() {
            runner.run(context -> assertThat(context)
                    .hasBean("eagleAmqpListenerContainerFactory")
                    .hasBean("eagleAmqpDlqListenerContainerFactory"));
        }
    }

    @Nested
    @DisplayName("标准配置键必须真正生效")
    class BootPropertiesTakeEffect {

        @Test
        @DisplayName("spring.rabbitmq.listener.simple.prefetch 能落到容器工厂上")
        void shouldApplyPrefetchFromStandardProperty() {
            runner.withPropertyValues("spring.rabbitmq.listener.simple.prefetch=7")
                    .run(context -> {
                        SimpleRabbitListenerContainerFactory factory = context.getBean(
                                "eagleAmqpListenerContainerFactory",
                                SimpleRabbitListenerContainerFactory.class);
                        assertThat(factory).extracting("prefetchCount").isEqualTo(7);
                    });
        }

        @Test
        @DisplayName("default-requeue-rejected 默认 false，但使用方显式配 true 时以使用方为准")
        void shouldDefaultRequeueRejectedToFalseYetStayOverridable() {
            runner.run(context -> assertThat(context.getBean(
                    "eagleAmqpListenerContainerFactory", SimpleRabbitListenerContainerFactory.class))
                    .extracting("defaultRequeueRejected").isEqualTo(false));

            runner.withPropertyValues("spring.rabbitmq.listener.simple.default-requeue-rejected=true")
                    .run(context -> assertThat(context.getBean(
                            "eagleAmqpListenerContainerFactory", SimpleRabbitListenerContainerFactory.class))
                            .extracting("defaultRequeueRejected").isEqualTo(true));
        }
    }

    @Nested
    @DisplayName("starter 级默认值")
    class StarterDefaults {

        @Test
        @DisplayName("默认打开重试：Boot 的 retry.enabled 默认是 false，不补这一手重试会静默消失")
        void shouldEnableRetryByDefault() {
            MockEnvironment environment = new MockEnvironment();
            new EagleAmqpDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("spring.rabbitmq.listener.simple.retry.enabled"))
                    .isEqualTo("true");
            assertThat(environment.getProperty("spring.rabbitmq.listener.simple.retry.max-retries"))
                    .isEqualTo("3");
            assertThat(environment.getProperty("spring.rabbitmq.template.retry.enabled"))
                    .isEqualTo("true");
        }

        @Test
        @DisplayName("默认值优先级最低：使用方配了就用使用方的")
        void shouldLoseToExplicitUserConfiguration() {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("spring.rabbitmq.listener.simple.retry.max-retries", "9");
            new EagleAmqpDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("spring.rabbitmq.listener.simple.retry.max-retries"))
                    .isEqualTo("9");
        }
    }
}

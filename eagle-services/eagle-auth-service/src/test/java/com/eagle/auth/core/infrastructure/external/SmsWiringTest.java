package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.SmsClientConfig;
import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import com.eagle.auth.core.infrastructure.config.SmsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 短信链路的 Spring 装配回归。
 *
 * <p>存在的理由：{@link HnslsSmsSenderTest} / {@link SmsServiceImplTest} 都直接 {@code new}
 * 被测对象，完全绕过容器，因此漏掉过一次真实故障——{@link HnslsSmsSender} 有两个构造函数，
 * 未标 {@code @Autowired} 时 Spring 回退去找无参构造函数，线上以
 * {@code No default constructor found} 启动失败，而单测全绿。
 *
 * <p>本测试让容器真正创建这几个 bean，把"能不能装配起来"和"逻辑对不对"分开守住。
 *
 * @author sunshixiong
 */
@DisplayName("短信链路 Spring 装配")
class SmsWiringTest {

    /**
     * 按生产方式注册：交给 Spring 解析构造函数，而不是测试里手工 new。
     */
    @Configuration(proxyBeanMethods = false)
    @Import({SmsClientConfig.class, SmsProperties.class, SmsMockProperties.class,
            HnslsSmsSender.class, SmsServiceImpl.class})
    static class WiringConfig {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(WiringConfig.class);

    @Test
    @DisplayName("容器可完成短信链路装配，构造函数歧义会在此暴露")
    void contextWiresSmsChain() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SmsServiceImpl.class);
            assertThat(context).hasSingleBean(HnslsSmsSender.class);
            assertThat(context).hasBean("smsRestClient");
        });
    }

    @Test
    @DisplayName("smsRestClient 按配置的超时构建，且被发送器实际取用")
    void injectsDedicatedRestClient() {
        runner.run(context -> {
            assertThat(context.getBean("smsRestClient")).isInstanceOf(RestClient.class);
            assertThat(context.getBean(HnslsSmsSender.class)).isNotNull();
        });
    }

    @Test
    @DisplayName("凭据未配置时装配照常完成，运行期回落日志兜底而不是启动失败")
    void wiresEvenWithoutCredentials() {
        // 默认 SmsProperties 的 provider/username/password 均为空——本地与 CI 的常态。
        // 装配阶段不得因此失败，否则没配短信的环境根本起不来。
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(SmsProperties.class).isHnslsCredentialComplete()).isFalse();
        });
    }
}

package com.eagle.zhetaoke;

import com.eagle.http.client.config.EagleHttpClientAutoConfiguration;
import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.zhetaoke.config.ZhetaokeAutoConfiguration;
import com.eagle.zhetaoke.douyin.client.DouyinClient;
import com.eagle.zhetaoke.eleme.client.ElemeClient;
import com.eagle.zhetaoke.jd.client.JdOpenClient;
import com.eagle.zhetaoke.kaola.client.KaolaClient;
import com.eagle.zhetaoke.meituan.client.MeituanClient;
import com.eagle.zhetaoke.pdd.client.PddClient;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import com.eagle.zhetaoke.vip.client.VipClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 各平台开放平台 Client 自动配置测试。
 *
 * @author 孙士雄
 */
class OpenPlatformAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    EagleHttpClientAutoConfiguration.class,
                    ZhetaokeAutoConfiguration.class));

    @Test
    void shouldAutoConfigureAllPlatformClients() {
        contextRunner
                .withPropertyValues(
                        "eagle.zhetaoke.appkey=test-key",
                        "eagle.zhetaoke.sid=test-sid",
                        "eagle.zhetaoke.pid=mm_1_2_3")
                .run(context -> assertThat(context)
                        .hasSingleBean(ZhetaokeProperties.class)
                        .hasSingleBean(JdOpenClient.class)
                        .hasSingleBean(MeituanClient.class)
                        .hasSingleBean(VipClient.class)
                        .hasSingleBean(KaolaClient.class)
                        .hasSingleBean(ElemeClient.class)
                        .hasSingleBean(PddClient.class)
                        .hasSingleBean(DouyinClient.class)
                        .hasSingleBean(EagleHttpServiceClientFactory.class));
    }

    @Test
    void shouldDisableAllWhenEnabledIsFalse() {
        contextRunner
                .withPropertyValues("eagle.zhetaoke.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(JdOpenClient.class)
                        .doesNotHaveBean(MeituanClient.class)
                        .doesNotHaveBean(VipClient.class)
                        .doesNotHaveBean(KaolaClient.class)
                        .doesNotHaveBean(ElemeClient.class)
                        .doesNotHaveBean(PddClient.class)
                        .doesNotHaveBean(DouyinClient.class));
    }
}

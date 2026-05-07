package com.eagle.zhetaoke.jd;

import com.eagle.zhetaoke.jd.properties.ZhejingkeProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ZhejingkeProperties} 配置属性测试。
 *
 * @author 孙士雄
 */
class ZhejingkePropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        ZhejingkeProperties properties = new ZhejingkeProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBaseUrl()).isEqualTo("http://api.zhetaoke.com:20000");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getAppkey()).isNull();
    }

    @Test
    void shouldSetCustomValues() {
        ZhejingkeProperties properties = new ZhejingkeProperties();

        properties.setEnabled(false);
        properties.setAppkey("test-appkey");
        properties.setBaseUrl("https://custom.api.com");
        properties.setConnectTimeout(Duration.ofSeconds(3));
        properties.setReadTimeout(Duration.ofSeconds(10));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAppkey()).isEqualTo("test-appkey");
        assertThat(properties.getBaseUrl()).isEqualTo("https://custom.api.com");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void shouldHandleEmptyStrings() {
        ZhejingkeProperties properties = new ZhejingkeProperties();

        properties.setAppkey("");

        assertThat(properties.getAppkey()).isEmpty();
    }

    @Test
    void shouldHandleNullValues() {
        ZhejingkeProperties properties = new ZhejingkeProperties();

        properties.setAppkey(null);
        properties.setBaseUrl(null);

        assertThat(properties.getAppkey()).isNull();
        assertThat(properties.getBaseUrl()).isNull();
    }
}

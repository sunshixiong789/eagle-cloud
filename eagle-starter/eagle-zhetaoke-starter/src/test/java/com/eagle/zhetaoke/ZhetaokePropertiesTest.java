package com.eagle.zhetaoke;

import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ZhetaokeProperties} 配置属性测试。
 *
 * @author 孙士雄
 */
class ZhetaokePropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        ZhetaokeProperties properties = new ZhetaokeProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.zhetaoke.com:10001");
        assertThat(properties.getBackupUrl()).isEqualTo("http://api.zhetaoke.cn:10000");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getAppkey()).isNull();
        assertThat(properties.getSid()).isNull();
        assertThat(properties.getPid()).isNull();
    }

    @Test
    void shouldSetCustomValues() {
        ZhetaokeProperties properties = new ZhetaokeProperties();

        properties.setEnabled(false);
        properties.setAppkey("test-appkey");
        properties.setSid("test-sid");
        properties.setPid("mm_1_2_3");
        properties.setBaseUrl("https://custom.api.com");
        properties.setBackupUrl("https://backup.api.com");
        properties.setConnectTimeout(Duration.ofSeconds(3));
        properties.setReadTimeout(Duration.ofSeconds(10));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAppkey()).isEqualTo("test-appkey");
        assertThat(properties.getSid()).isEqualTo("test-sid");
        assertThat(properties.getPid()).isEqualTo("mm_1_2_3");
        assertThat(properties.getBaseUrl()).isEqualTo("https://custom.api.com");
        assertThat(properties.getBackupUrl()).isEqualTo("https://backup.api.com");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void shouldHandleEmptyStrings() {
        ZhetaokeProperties properties = new ZhetaokeProperties();

        properties.setAppkey("");
        properties.setSid("");
        properties.setPid("");

        assertThat(properties.getAppkey()).isEmpty();
        assertThat(properties.getSid()).isEmpty();
        assertThat(properties.getPid()).isEmpty();
    }

    @Test
    void shouldHandleNullValues() {
        ZhetaokeProperties properties = new ZhetaokeProperties();

        properties.setAppkey(null);
        properties.setSid(null);
        properties.setPid(null);
        properties.setBaseUrl(null);
        properties.setBackupUrl(null);

        assertThat(properties.getAppkey()).isNull();
        assertThat(properties.getSid()).isNull();
        assertThat(properties.getPid()).isNull();
        assertThat(properties.getBaseUrl()).isNull();
        assertThat(properties.getBackupUrl()).isNull();
    }
}

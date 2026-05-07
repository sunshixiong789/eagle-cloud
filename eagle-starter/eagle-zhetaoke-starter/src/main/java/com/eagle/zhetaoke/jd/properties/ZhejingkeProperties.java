package com.eagle.zhetaoke.jd.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 折京客 API 配置属性。
 *
 * <p>典型配置示例：
 * <pre>{@code
 * eagle:
 *   zhejingke:
 *     enabled: true
 *     appkey: your-appkey
 *     base-url: http://api.zhetaoke.com:20000
 *     connect-timeout: 2s
 *     read-timeout: 5s
 * }}</pre>
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.zhejingke")
public class ZhejingkeProperties {

    /** 是否启用折京客自动配置，默认开启。 */
    private boolean enabled = true;

    /** 折京客对接秘钥 appkey（必填）。 */
    private String appkey;

    /** 接口域名，默认 http://api.zhetaoke.com:20000。 */
    private String baseUrl = "http://api.zhetaoke.com:20000";

    /** 连接超时，默认 2 秒。 */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /** 读取超时，默认 5 秒。 */
    private Duration readTimeout = Duration.ofSeconds(5);
}

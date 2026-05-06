package com.eagle.zhetaoke.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 折淘客 API 配置属性。
 *
 * <p>典型配置示例：
 * <pre>{@code
 * eagle:
 *   zhetaoke:
 *     enabled: true
 *     appkey: your-appkey
 *     sid: your-sid
 *     pid: mm_xxx_xxx_xxx
 *     base-url: https://api.zhetaoke.com:10001
 *     backup-url: http://api.zhetaoke.cn:10000
 *     connect-timeout: 2s
 *     read-timeout: 5s
 * }}</pre>
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.zhetaoke")
public class ZhetaokeProperties {

    /** 是否启用折淘客自动配置，默认开启。 */
    private boolean enabled = true;

    /** 折淘客对接秘钥 appkey（必填）。 */
    private String appkey;

    /** 淘客账号授权 ID sid（必填）。 */
    private String sid;

    /** 淘客 PID，格式 mm_xxx_xxx_xxx（必填）。 */
    private String pid;

    /** 主接口域名，默认 https://api.zhetaoke.com:10001。 */
    private String baseUrl = "https://api.zhetaoke.com:10001";

    /** 备用接口域名，默认 http://api.zhetaoke.cn:10000。 */
    private String backupUrl = "http://api.zhetaoke.cn:10000";

    /** 连接超时，默认 2 秒。 */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /** 读取超时，默认 5 秒。 */
    private Duration readTimeout = Duration.ofSeconds(5);
}

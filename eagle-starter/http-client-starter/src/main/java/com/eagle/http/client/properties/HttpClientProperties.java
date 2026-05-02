package com.eagle.http.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Eagle HTTP 客户端配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.http-client")
public class HttpClientProperties {

    /**
     * 连接超时，默认 2 秒。
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * 读取超时，默认 5 秒。
     */
    private Duration readTimeout = Duration.ofSeconds(5);

    /**
     * 是否启用项目统一错误转换。
     */
    private boolean errorHandlerEnabled = true;

    /**
     * 是否启用请求/响应体缓冲，便于错误处理和日志组件重复读取。
     */
    private boolean bufferContent = true;

    /**
     * 是否透传全链路压测 Header。
     */
    private boolean pressureTestHeaderEnabled = true;

    /**
     * 从当前入站 Servlet 请求自动透传到下游的 Header。
     */
    private List<String> propagatedHeaders = new ArrayList<>(
            List.of("Authorization", "Accept-Language", "X-Request-Id", "X-Correlation-Id"));
}

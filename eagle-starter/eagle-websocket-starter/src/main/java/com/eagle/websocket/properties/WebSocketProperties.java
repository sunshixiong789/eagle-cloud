package com.eagle.websocket.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * WebSocket 配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.websocket")
public class WebSocketProperties {

    /** 允许跨域的来源，默认允许所有（生产环境建议配置为具体域名） */
    private List<String> allowedOrigins = List.of("*");

    /** STOMP WebSocket 端点路径 */
    private String endpoint = "/ws";

    /** 广播消息目标前缀（客户端订阅 /topic/xxx） */
    private String topicPrefix = "/topic";

    /** 用户专属消息前缀（服务端推送 /user/{userId}/queue/xxx） */
    private String userPrefix = "/user";

    /** 客户端发送消息的应用前缀（@MessageMapping 路径前缀） */
    private String appPrefix = "/app";

    /** 心跳间隔（ms），0 表示禁用 */
    private long heartbeatMs = 10000;
}

package com.eagle.system.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 可信反向代理配置。
 *
 * <p>仅当请求的直连远端 IP 命中可信 CIDR 列表时，{@code X-Forwarded-For} 才会被采纳；
 * 否则一律使用 {@code request.getRemoteAddr()}，防止外部直接构造伪造头绕过 IP 限流 / 黑名单。
 *
 * <p>配置示例：
 * <pre>
 * eagle:
 *   security:
 *     trusted-proxies:
 *       - 127.0.0.1/32
 *       - 10.0.0.0/8
 *       - 172.16.0.0/12
 *       - 192.168.0.0/16
 * </pre>
 *
 * <p>未配置时默认信任本地回环 + RFC1918 私网（典型 K8s / Docker 网络），生产环境强烈建议显式收敛到反向代理的具体地址。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.security")
public class TrustedProxyProperties {

    /**
     * 可信代理 CIDR 列表（IPv4 / IPv6）；空列表表示完全不信任 X-Forwarded-For
     */
    private List<String> trustedProxies = new ArrayList<>(List.of(
            "127.0.0.0/8",
            "::1/128",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16"
    ));
}

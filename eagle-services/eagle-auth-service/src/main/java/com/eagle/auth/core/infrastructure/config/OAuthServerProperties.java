package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth2 Authorization Server 服务端级别配置属性。
 *
 * <p>对应 application.yml 中 {@code eagle.oauth} 前缀下的服务端字段（与
 * {@link OAuthClientProperties} / {@link OAuthAppClientProperties} 的客户端字段平级）。</p>
 *
 * <p>当前包含：
 * <ul>
 *   <li>{@code issuer} — 锁定签发方身份。一旦设置，Spring Authorization Server 不再从
 *       {@code X-Forwarded-Host} 派生 issuer,所有 token 的 {@code iss} claim 字面恒等于此值,
 *       避免随反向代理/前端入口/集群实例漂移。资源服务器据此配置 {@code issuer-uri} 即可。</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.oauth")
public class OAuthServerProperties {

    /**
     * OAuth2 Authorization Server 对外 issuer。
     *
     * <p>必填值,该字段必须满足:
     * <ul>
     *   <li>所有签发 token 的 system 实例配置完全相同(集群部署的前提)</li>
     *   <li>从浏览器和后端资源服务器都能解析到 {@code ${issuer}/.well-known/openid-configuration}</li>
     *   <li>典型取值: LB / 网关给客户端的对外稳定 URL,而非任何单实例容器地址</li>
     * </ul>
     */
    private String issuer;
}

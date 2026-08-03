package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * OAuth2 运营端机密客户端配置属性。
 *
 * <p>对应 application.yml 中的 {@code eagle.oauth.ops-client} 前缀配置，
 * 供内部运营系统以 {@code client_credentials} 换取 access token 调用受保护的对外接口
 * （如按手机号发放 / 作废购物金）。
 *
 * <p>与 web / app 两个客户端的关键区别：
 * <ul>
 *   <li><strong>机密客户端</strong>——带 secret，走 {@code client_secret_basic}，
 *       因此不经过 {@code CustomGrantClientAuthenticationProvider} /
 *       {@code CustomGrantPublicClientAuthenticationConverter}
 *       （二者只接管 {@code ClientAuthenticationMethod.NONE}），无需登记进它们的 grant 白名单。</li>
 *   <li><strong>无用户上下文</strong>——{@code client_credentials} 签发的 token 没有 user principal，
 *       资源服务器侧 {@code SecurityUtils.getCurrentUserId()} 为 null，业务须改用 client_id 作为操作人标识。</li>
 *   <li><strong>无浏览器回跳</strong>——不配置 redirect_uri / post_logout_redirect_uri，不需要 PKCE 与授权确认页。</li>
 * </ul>
 *
 * <p>secret 必须由部署环境注入（{@code EAGLE_OAUTH_OPS_CLIENT_CLIENT_SECRET}），
 * 默认留空表示未配置——此时应关闭 {@code enabled}，避免建出无凭据的客户端。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.oauth.ops-client")
public class OAuthOpsClientProperties {

    /** 默认关闭：需要显式开启并注入 secret 后才建号，避免测试 / 本地环境凭空多出机密客户端。 */
    private boolean enabled = false;

    /**
     * 启动时与 DB 已有配置的同步策略；详见 {@link OAuthClientProperties#getSyncMode}。
     */
    private SyncMode syncMode = SyncMode.OVERWRITE;

    private String clientId = "shengxinOps";

    private String clientName = "省心运营系统";

    /** 客户端密钥，必须由环境变量注入，禁止写入仓库。 */
    private String clientSecret = "";

    private Set<String> clientAuthenticationMethods = Set.of("client_secret_basic");

    private Set<String> authorizationGrantTypes = Set.of("client_credentials");

    /**
     * 购物金对外接口权限。发放与作废分开，作废（清空用户全部可用余额）风险更高，
     * 可按调用方按需只授予其中之一。
     */
    private Set<String> scopes = Set.of("shopping-gold.grant", "shopping-gold.revoke");

    /** client_credentials 不签发 refresh_token，token 短 TTL 由调用方到期重新换取。 */
    private long accessTokenTtlSeconds = 1800L;
}

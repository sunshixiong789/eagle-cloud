package com.eagle.auth.core.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;
import java.util.Set;

/**
 * 自定义 grant_type 的 public client 认证 Provider。
 *
 * <p>用于 SAS 7.0 中绕过 {@code PublicClientAuthenticationProvider} 对 public client
 * 的强制 PKCE 校验。</p>
 *
 * <p>本 Provider 接管两类公共客户端认证，均自行校验 client_id 注册与认证方法，校验通过直接返回
 * 已认证的 {@link OAuth2ClientAuthenticationToken}，跳过 PKCE 检查：</p>
 * <ul>
 *   <li>{@code sms_code} / {@code wechat_app} / {@code wechat_mini_program} / {@code phone_one_click} /
 *       {@code taobao_app} 等自定义 grant_type：额外校验 grant_type 白名单；</li>
 *   <li>{@code /oauth2/revoke}（退出登录）：无 grant_type 但带 {@code token} 参数，不校验 grant 白名单
 *       （revoke 不是授权类型）。introspect 不在此列。</li>
 * </ul>
 * <p>其它情况返回 {@code null}，由内置 {@code PublicClientAuthenticationProvider} 继续接管
 * （保留 web 授权码流程的 PKCE 保护）。</p>
 *
 * <p>必须在 SAS DSL 中通过 {@code .clientAuthentication(c -> c.authenticationProvider(...))}
 * 注入，DSL 会把它前置到 {@code PublicClientAuthenticationProvider} 之前。</p>
 *
 * @author sunshixiong
 */
@Slf4j
@RequiredArgsConstructor
public class CustomGrantClientAuthenticationProvider implements AuthenticationProvider {

    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
            SmsCodeAuthenticationToken.SMS_CODE.getValue(),
            WechatAppAuthenticationToken.WECHAT_APP.getValue(),
            WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM.getValue(),
            PhoneOneClickAuthenticationToken.PHONE_ONE_CLICK.getValue(),
            TaobaoAppAuthenticationToken.TAOBAO_APP.getValue(),
            // 与 CustomGrantPublicClientAuthenticationConverter 对齐：放行公共客户端 refresh_token，
            // 跳过 PKCE 校验；后续 SAS 标准 RefreshTokenAuthenticationProvider 仍会校验 refresh_token 有效性。
            AuthorizationGrantType.REFRESH_TOKEN.getValue()
    );

    private final RegisteredClientRepository registeredClientRepository;

    @Override
    public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken token = (OAuth2ClientAuthenticationToken) authentication;

        // 只接管 NONE 认证方法
        if (!ClientAuthenticationMethod.NONE.equals(token.getClientAuthenticationMethod())) {
            return null;
        }

        Map<String, Object> params = token.getAdditionalParameters();
        Object grantTypeObj = params == null ? null : params.get(OAuth2ParameterNames.GRANT_TYPE);
        String grantType = grantTypeObj == null ? null : grantTypeObj.toString();

        boolean supportedGrant = grantType != null && SUPPORTED_GRANT_TYPES.contains(grantType);
        // revoke 端点公共客户端认证：无 grant_type 但带 token 参数
        // （converter 已按 revocation 端点 URI 限定，introspect 不在此列）。
        boolean publicClientRevoke = grantType == null
                && params != null && params.get(OAuth2ParameterNames.TOKEN) != null;

        // 既非自定义 grant_type、也非 revoke：放行给内置 PublicClientAuthenticationProvider 处理 PKCE 校验
        if (!supportedGrant && !publicClientRevoke) {
            return null;
        }

        String clientId = token.getPrincipal() == null ? null : token.getPrincipal().toString();
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_CLIENT, "client_id not registered: " + clientId, null));
        }
        if (!client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_CLIENT, "authentication_method", null));
        }
        // grant_type 白名单只校验授权请求；revoke 不是授权类型，跳过 grant 校验。
        if (supportedGrant
                && !client.getAuthorizationGrantTypes().contains(new AuthorizationGrantType(grantType))) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "grant_type not allowed: " + grantType, null));
        }

        // 已认证（带 RegisteredClient 构造器使 isAuthenticated() = true），跳过 PKCE
        return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

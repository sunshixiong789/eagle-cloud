package com.eagle.system.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 的强制 PKCE 校验：SAS 7.0 内置的 {@code CodeVerifierAuthenticator.authenticateRequired}
 * 对所有 NONE 认证方法的 client 强制要求 {@code code_verifier} 参数，
 * 与 client 的 {@code require-proof-key} 设置无关（require-proof-key 只影响
 * {@code /oauth2/authorize} 入口的授权码颁发，不影响 {@code /oauth2/token} 客户端认证）。</p>
 *
 * <p>本 Provider 仅接管 {@code sms_code} / {@code wechat_app} / {@code wechat_mini_program} /
 * {@code phone_one_click} 三种自定义 grant_type 的客户端认证：自行校验 client_id 注册、
 * 认证方法、grant_type 白名单，校验通过直接返回已认证的 {@link OAuth2ClientAuthenticationToken}，
 * 跳过 PKCE 检查。其它 grant_type 返回 {@code null}，由内置
 * {@code PublicClientAuthenticationProvider} 继续接管（保留 web 授权码流程的 PKCE 保护）。</p>
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
            PhoneOneClickAuthenticationToken.PHONE_ONE_CLICK.getValue()
    );

    private final RegisteredClientRepository registeredClientRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken token = (OAuth2ClientAuthenticationToken) authentication;

        // 只接管 NONE 认证方法
        if (!ClientAuthenticationMethod.NONE.equals(token.getClientAuthenticationMethod())) {
            return null;
        }

        Map<String, Object> params = token.getAdditionalParameters();
        Object grantTypeObj = params == null ? null : params.get(OAuth2ParameterNames.GRANT_TYPE);
        String grantType = grantTypeObj == null ? null : grantTypeObj.toString();

        // 非自定义 grant_type 放行给内置 PublicClientAuthenticationProvider 处理 PKCE 校验
        if (grantType == null || !SUPPORTED_GRANT_TYPES.contains(grantType)) {
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
        if (!client.getAuthorizationGrantTypes().contains(new AuthorizationGrantType(grantType))) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "grant_type not allowed: " + grantType, null));
        }

        // 已认证（带 RegisteredClient 构造器使 isAuthenticated() = true），跳过 PKCE
        return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

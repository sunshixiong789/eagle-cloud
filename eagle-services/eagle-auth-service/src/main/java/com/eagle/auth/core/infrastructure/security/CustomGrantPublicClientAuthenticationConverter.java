package com.eagle.auth.core.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 自定义 grant_type 的 public client 认证转换器。
 * <p>
 * Spring Authorization Server 内置的 {@code PublicClientAuthenticationConverter} 仅识别
 * {@code grant_type=authorization_code} + PKCE {@code code_verifier} 的请求。
 * 对于本项目自定义的 sms_code / wechat_app / wechat_mini_program / phone_one_click / taobao_app 等 grant_type,
 * 内置 converter 全部返回 null,导致 public client(无 secret)无法通过 client 认证,
 * token endpoint 抛 {@code invalid_client}。
 * <p>
 * 本转换器对这些自定义 grant_type 放行 public client 认证:只要 client_id 存在,
 * 即构造 {@link ClientAuthenticationMethod#NONE} 的 {@link OAuth2ClientAuthenticationToken}。
 * 后续 {@code PublicClientAuthenticationProvider} 仍会校验 client_id 是否注册、
 * authentication-methods 是否包含 NONE、以及 grant_type 是否在该 client 的允许清单内。
 * <p>
 * 同理, {@code /oauth2/revoke}(退出登录撤销 token)请求不带 grant_type、也不带 PKCE/secret,
 * 同样会 fallback 到 {@code invalid_client}(中台退出报「无效的 client」)。本转换器按 revocation
 * 端点 URI + 无凭据识别后,同样构造 NONE 的认证令牌放行。<strong>不</strong>覆盖 introspect 端点,
 * 避免扩大令牌内省的访问面。
 *
 * @author sunshixiong
 */
public final class CustomGrantPublicClientAuthenticationConverter implements AuthenticationConverter {

    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
            SmsCodeAuthenticationToken.SMS_CODE.getValue(),
            WechatAppAuthenticationToken.WECHAT_APP.getValue(),
            WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM.getValue(),
            PhoneOneClickAuthenticationToken.PHONE_ONE_CLICK.getValue(),
            TaobaoAppAuthenticationToken.TAOBAO_APP.getValue(),
            AppleAppAuthenticationToken.APPLE_APP.getValue(),
            SocialBindAuthenticationToken.SOCIAL_BIND.getValue(),
            // 公共客户端（无 secret）刷新 token：SAS 内置 4 个 client auth converter 都不识别
            // grant_type=refresh_token + 无 secret + 无 PKCE 的组合，因此 fallback 到 invalid_client。
            // 这里把 refresh_token 也放进自定义 public client 链路。
            AuthorizationGrantType.REFRESH_TOKEN.getValue()
    );

    /**
     * token revocation 端点路径（取自 {@code AuthorizationServerSettings}，默认 {@code /oauth2/revoke}）。
     * <p>revoke 请求不带 {@code grant_type}，无法走上面的 grant_type 白名单；改为按端点 URI 识别。
     * 故意<strong>不</strong>覆盖 introspection 端点：放开公共客户端 introspection 会扩大令牌内省的访问面，
     * 而 web 退出登录只需要 revoke。
     */
    private final String tokenRevocationEndpoint;

    public CustomGrantPublicClientAuthenticationConverter(String tokenRevocationEndpoint) {
        this.tokenRevocationEndpoint = tokenRevocationEndpoint;
    }

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        boolean supportedGrant = grantType != null && SUPPORTED_GRANT_TYPES.contains(grantType);
        // revoke 请求不带 grant_type，无法走 grant_type 白名单：按 revocation 端点 + 无凭据识别后放行。
        boolean publicClientRevoke = grantType == null && isCredentiallessRevoke(request);
        if (!supportedGrant && !publicClientRevoke) {
            return null;
        }

        String[] clientIds = request.getParameterValues(OAuth2ParameterNames.CLIENT_ID);
        if (clientIds == null || clientIds.length != 1 || !StringUtils.hasText(clientIds[0])) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }
        String clientId = clientIds[0];

        Map<String, Object> additionalParameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (OAuth2ParameterNames.CLIENT_ID.equals(key)) {
                return;
            }
            if (values.length == 1) {
                additionalParameters.put(key, values[0]);
            } else {
                additionalParameters.put(key, values);
            }
        });

        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null,
                additionalParameters);
    }

    /**
     * 是否为「无 client 凭据的公共客户端 revoke 请求」。
     * <p>同时满足：命中 revocation 端点、带 {@code token} 参数、且不携带任何 client 凭据
     * （Basic 头 / {@code client_secret} / {@code client_assertion}）。携带凭据的请求一律返回 false，
     * 交给 SAS 内置的 secret / JWT-assertion converter，避免误接管机密客户端的 revoke。
     */
    private boolean isCredentiallessRevoke(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.endsWith(tokenRevocationEndpoint)) {
            return false;
        }
        if (!StringUtils.hasText(request.getParameter(OAuth2ParameterNames.TOKEN))) {
            return false;
        }
        return !StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION))
                && !StringUtils.hasText(request.getParameter(OAuth2ParameterNames.CLIENT_SECRET))
                && !StringUtils.hasText(request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION));
    }
}

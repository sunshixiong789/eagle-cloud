package com.eagle.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
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
 * 对于本项目自定义的 sms_code / wechat_app / wechat_mini_program / phone_one_click 等 grant_type,
 * 内置 converter 全部返回 null,导致 public client(无 secret)无法通过 client 认证,
 * token endpoint 抛 {@code invalid_client}。
 * <p>
 * 本转换器对这些自定义 grant_type 放行 public client 认证:只要 client_id 存在,
 * 即构造 {@link ClientAuthenticationMethod#NONE} 的 {@link OAuth2ClientAuthenticationToken}。
 * 后续 {@code PublicClientAuthenticationProvider} 仍会校验 client_id 是否注册、
 * authentication-methods 是否包含 NONE、以及 grant_type 是否在该 client 的允许清单内。
 *
 * @author sunshixiong
 */
public final class CustomGrantPublicClientAuthenticationConverter implements AuthenticationConverter {

    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
            SmsCodeAuthenticationToken.SMS_CODE.getValue(),
            WechatAppAuthenticationToken.WECHAT_APP.getValue(),
            WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM.getValue(),
            PhoneOneClickAuthenticationToken.PHONE_ONE_CLICK.getValue(),
            // 公共客户端（无 secret）刷新 token：SAS 内置 4 个 client auth converter 都不识别
            // grant_type=refresh_token + 无 secret + 无 PKCE 的组合，因此 fallback 到 invalid_client。
            // 这里把 refresh_token 也放进自定义 public client 链路。
            AuthorizationGrantType.REFRESH_TOKEN.getValue()
    );

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (grantType == null || !SUPPORTED_GRANT_TYPES.contains(grantType)) {
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
}

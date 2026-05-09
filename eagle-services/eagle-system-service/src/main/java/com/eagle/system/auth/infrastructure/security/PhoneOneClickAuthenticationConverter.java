package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.domain.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * 手机号一键登录请求转换器
 * <p>
 * 从 HTTP 请求中提取 {@code grant_type=phone_one_click} 的参数。
 * 期望参数：
 * <ul>
 *   <li>{@code grant_type=phone_one_click}</li>
 *   <li>{@code access_token}：运营商 / SDK 颁发的一键登录 token</li>
 * </ul>
 *
 * @author sunshixiong
 */
public class PhoneOneClickAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!PhoneOneClickAuthenticationToken.PHONE_ONE_CLICK.getValue().equals(grantType)) {
            return null;
        }

        String accessToken = request.getParameter("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw AuthErrorCode.ONE_CLICK_TOKEN_REQUIRED.toDomainException();
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("access_token", accessToken);

        return new PhoneOneClickAuthenticationToken(accessToken, clientPrincipal, additionalParameters);
    }
}

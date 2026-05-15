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
 * 微信 App 登录请求转换器。
 *
 * @author sunshixiong
 */
public class WechatAppAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!WechatAppAuthenticationToken.WECHAT_APP.getValue().equals(grantType)) {
            return null;
        }

        String code = request.getParameter("code");
        if (code == null || code.isBlank()) {
            throw AuthErrorCode.WECHAT_CODE_REQUIRED.toDomainException();
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("code", code);

        return new WechatAppAuthenticationToken(code, clientPrincipal, additionalParameters);
    }
}

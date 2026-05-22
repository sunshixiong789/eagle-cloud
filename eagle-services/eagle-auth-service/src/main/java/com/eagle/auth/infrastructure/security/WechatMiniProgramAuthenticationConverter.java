package com.eagle.auth.infrastructure.security;

import com.eagle.auth.domain.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序登录请求转换器
 * <p>
 * 从 HTTP 请求中提取 grant_type=wechat_mini_program 的参数
 *
 * @author sunshixiong
 */
public class WechatMiniProgramAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM.getValue().equals(grantType)) {
            return null;
        }

        String code = request.getParameter("code");
        if (code == null || code.isBlank()) {
            throw AuthErrorCode.WECHAT_CODE_REQUIRED.toDomainException();
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("code", code);

        return new WechatMiniProgramAuthenticationToken(code, clientPrincipal, additionalParameters);
    }
}

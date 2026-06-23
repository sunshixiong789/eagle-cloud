package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.domain.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * 淘宝 App 登录请求转换器。参数名 tb_access_token / tb_auth_code / phone / sms_code。
 *
 * @author sunshixiong
 */
public class TaobaoAppAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!TaobaoAppAuthenticationToken.TAOBAO_APP.getValue().equals(grantType)) {
            return null;
        }

        String tbAccessToken = request.getParameter("tb_access_token");
        String tbAuthCode = request.getParameter("tb_auth_code");
        if ((tbAccessToken == null || tbAccessToken.isBlank())
                && (tbAuthCode == null || tbAuthCode.isBlank())) {
            throw AuthErrorCode.TAOBAO_AUTH_REQUIRED.toDomainException();
        }
        String phone = request.getParameter("phone");
        String smsCode = request.getParameter("sms_code");

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("tb_access_token", tbAccessToken);
        additionalParameters.put("tb_auth_code", tbAuthCode);

        return new TaobaoAppAuthenticationToken(
                tbAccessToken, tbAuthCode, phone, smsCode, clientPrincipal, additionalParameters);
    }
}

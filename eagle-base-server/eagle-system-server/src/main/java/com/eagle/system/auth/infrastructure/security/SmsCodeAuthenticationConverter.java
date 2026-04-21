package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.exception.codes.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信验证码登录请求转换器
 * <p>
 * 从 HTTP 请求中提取 grant_type=sms_code 的参数
 *
 * @author sunshixiong
 */
public class SmsCodeAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!SmsCodeAuthenticationToken.SMS_CODE.getValue().equals(grantType)) {
            return null;
        }

        String phone = request.getParameter("phone");
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.SMS_PHONE_REQUIRED.toDomainException();
        }

        String code = request.getParameter("code");
        if (code == null || code.isBlank()) {
            throw AuthErrorCode.SMS_CODE_REQUIRED.toDomainException();
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("phone", phone);
        additionalParameters.put("code", code);

        return new SmsCodeAuthenticationToken(phone, code, clientPrincipal, additionalParameters);
    }
}

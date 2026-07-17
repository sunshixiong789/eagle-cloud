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
 * social_bind 请求转换器：提取 grant_type=social_bind 的
 * bind_ticket / phone / code 参数。
 *
 * @author sunshixiong
 */
public class SocialBindAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!SocialBindAuthenticationToken.SOCIAL_BIND.getValue().equals(grantType)) {
            return null;
        }

        String bindTicket = request.getParameter("bind_ticket");
        if (bindTicket == null || bindTicket.isBlank()) {
            throw AuthErrorCode.SOCIAL_BIND_TICKET_INVALID.toDomainException();
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
        additionalParameters.put("bind_ticket", bindTicket);
        additionalParameters.put("phone", phone);
        additionalParameters.put("code", code);

        return new SocialBindAuthenticationToken(
                bindTicket, phone, code, clientPrincipal, additionalParameters);
    }
}

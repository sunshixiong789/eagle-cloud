package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.domain.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.HashMap;
import java.util.Map;

/** Apple App 登录请求转换器。 */
public class AppleAppAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!AppleAppAuthenticationToken.APPLE_APP.getValue().equals(grantType)) {
            return null;
        }

        String identityToken = request.getParameter("identity_token");
        if (identityToken == null || identityToken.isBlank()) {
            throw AuthErrorCode.APPLE_IDENTITY_TOKEN_REQUIRED.toDomainException();
        }
        String nonce = request.getParameter("nonce");
        if (nonce == null || nonce.isBlank()) {
            throw AuthErrorCode.APPLE_NONCE_REQUIRED.toDomainException();
        }
        String fullName = normalizeFullName(request.getParameter("full_name"));
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("identity_token", identityToken);
        additionalParameters.put("nonce", nonce);
        return new AppleAppAuthenticationToken(
                identityToken, nonce, fullName, clientPrincipal, additionalParameters);
    }

    private String normalizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String normalized = fullName.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}

package com.eagle.system.auth.infrastructure.security;

import com.alibaba.fastjson2.JSON;
import com.eagle.auth.domain.port.OnlineUserInfo;
import com.eagle.auth.domain.port.OnlineUserPort;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * OAuth2 token endpoint 登录成功处理器。
 * <p>
 * 颁发 access token 后将在线用户信息写入 Redis，并写出标准 OAuth2 token 响应。
 * 注意：注册 {@code accessTokenResponseHandler} 会完全替换框架默认响应写出逻辑，
 * 必须自行写出 token 响应（使用 {@link OAuth2AccessTokenResponseHttpMessageConverter}）。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenTrackingHandler implements AuthenticationSuccessHandler {

    private final OnlineUserPort onlineUserPort;

    private final HttpMessageConverter<OAuth2AccessTokenResponse>
        tokenResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuth)) {
            return;
        }

        // 1. Write standard token response
        OAuth2AccessTokenResponse tokenResponse = OAuth2AccessTokenResponse
            .withToken(tokenAuth.getAccessToken().getTokenValue())
            .tokenType(tokenAuth.getAccessToken().getTokenType())
            .scopes(tokenAuth.getAccessToken().getScopes())
            .expiresIn(tokenAuth.getAccessToken().getExpiresAt() != null
                ? Duration.between(Instant.now(), tokenAuth.getAccessToken().getExpiresAt()).getSeconds()
                : 3600L)
            .additionalParameters(tokenAuth.getAdditionalParameters() != null
                ? tokenAuth.getAdditionalParameters() : Map.of())
            .build();
        tokenResponseConverter.write(tokenResponse, MediaType.APPLICATION_JSON,
            new ServletServerHttpResponse(response));

        // 2. Track online user — failure must not affect the already-written token response
        try {
            trackOnlineUser(request, tokenAuth);
        } catch (Exception e) {
            log.warn("Failed to track online user, skipping: {}", e.getMessage());
        }
    }

    private void trackOnlineUser(HttpServletRequest request,
                                 OAuth2AccessTokenAuthenticationToken tokenAuth) {
        String tokenValue = tokenAuth.getAccessToken().getTokenValue();
        String jti = extractClaim(tokenValue, "jti");
        if (jti == null) {
            log.debug("JWT has no jti claim, skipping online user tracking");
            return;
        }

        String sub = extractClaim(tokenValue, "sub");
        Instant expiresAt = tokenAuth.getAccessToken().getExpiresAt();
        long expiresIn = expiresAt != null
            ? Math.max(Duration.between(Instant.now(), expiresAt).getSeconds(), 0L)
            : 3600L;

        String userAgent = request.getHeader("User-Agent");
        OnlineUserInfo info = new OnlineUserInfo(
            jti, null,
            sub != null ? sub : "unknown",
            getClientIp(request),
            LocalDateTime.now(), LocalDateTime.now(),
            parseBrowser(userAgent),
            parseOs(userAgent),
            expiresIn
        );
        onlineUserPort.trackLogin(info);
        log.debug("Tracked online user: {}, jti: {}", sub, jti);
    }

    /** 从 JWT payload 中提取指定 claim 值（无签名验证，仅用于非安全用途的元数据读取）。 */
    private String extractClaim(String jwtValue, String claimName) {
        try {
            String[] parts = jwtValue.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = parts[1];
            // 补齐 Base64 padding
            int mod = payload.length() % 4;
            if (mod == 2) {
                payload += "==";
            } else if (mod == 3) {
                payload += "=";
            }
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            Map<String, Object> claims = JSON.parseObject(decoded);
            Object val = claims.get(claimName);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to extract claim '{}' from JWT: {}", claimName, e.getMessage());
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String parseBrowser(String ua) {
        if (ua == null) {
            return "Unknown";
        }
        if (ua.contains("Edg")) {
            return "Edge";
        }
        if (ua.contains("Chrome")) {
            return "Chrome";
        }
        if (ua.contains("Firefox")) {
            return "Firefox";
        }
        if (ua.contains("Safari")) {
            return "Safari";
        }
        return "Unknown";
    }

    private String parseOs(String ua) {
        if (ua == null) {
            return "Unknown";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Macintosh") || ua.contains("Mac OS X")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("iPhone") || ua.contains("iPad")) {
            return "iOS";
        }
        return "Unknown";
    }
}

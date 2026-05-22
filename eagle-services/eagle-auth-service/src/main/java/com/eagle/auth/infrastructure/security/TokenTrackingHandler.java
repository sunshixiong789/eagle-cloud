package com.eagle.auth.infrastructure.security;

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
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth2 token endpoint 登录成功处理器。
 *
 * <p>颁发 access token 后将在线用户信息写入 Redis，并写出标准 OAuth2 token 响应。
 * 注意：注册 {@code accessTokenResponseHandler} 会完全替换框架默认响应写出逻辑，
 * 必须自行写出 token 响应（使用 {@link OAuth2AccessTokenResponseHttpMessageConverter}）。
 *
 * <p>jti / sub 等元数据从 {@link OAuth2Authorization} 的 claims metadata 中读取，不再
 * 裸解析 JWT —— 这些 claims 是认证流程中签名前就持久化好的可信数据。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenTrackingHandler implements AuthenticationSuccessHandler {

    private final OnlineUserPort onlineUserPort;
    private final OAuth2AuthorizationService authorizationService;

    private final HttpMessageConverter<OAuth2AccessTokenResponse>
            tokenResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuth)) {
            return;
        }

        OAuth2AccessTokenResponse.Builder responseBuilder = OAuth2AccessTokenResponse
                .withToken(tokenAuth.getAccessToken().getTokenValue())
                .tokenType(tokenAuth.getAccessToken().getTokenType())
                .scopes(tokenAuth.getAccessToken().getScopes())
                .expiresIn(tokenAuth.getAccessToken().getExpiresAt() != null
                        ? Duration.between(Instant.now(), tokenAuth.getAccessToken().getExpiresAt()).getSeconds()
                        : 3600L)
                .additionalParameters(tokenAuth.getAdditionalParameters() != null
                        ? tokenAuth.getAdditionalParameters() : Map.of());
        if (tokenAuth.getRefreshToken() != null) {
            responseBuilder.refreshToken(tokenAuth.getRefreshToken().getTokenValue());
        }
        tokenResponseConverter.write(responseBuilder.build(), MediaType.APPLICATION_JSON,
                new ServletServerHttpResponse(response));

        try {
            trackOnlineUser(request, tokenAuth);
        } catch (Exception e) {
            log.warn("failed to track online user, skipping", e);
        }
    }

    private void trackOnlineUser(HttpServletRequest request,
                                 OAuth2AccessTokenAuthenticationToken tokenAuth) {
        OAuth2AccessToken accessToken = tokenAuth.getAccessToken();
        OAuth2Authorization authorization = authorizationService
                .findByToken(accessToken.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            log.debug("authorization not found for token tracking, skipping");
            return;
        }
        OAuth2Authorization.Token<OAuth2AccessToken> tokenEntry =
                authorization.getToken(OAuth2AccessToken.class);
        Map<String, Object> claims = tokenEntry == null ? null : tokenEntry.getClaims();
        if (claims == null) {
            log.debug("no claims metadata for token tracking, skipping");
            return;
        }
        Object jtiObj = claims.get(JwtClaimNames.JTI);
        if (jtiObj == null) {
            log.debug("no jti claim, skipping online user tracking");
            return;
        }

        String jti = jtiObj.toString();
        String sub = authorization.getPrincipalName();
        Instant expiresAt = accessToken.getExpiresAt();
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

    private String getClientIp(HttpServletRequest request) {
        String ip = ClientIpHolder.get();
        return ip != null ? ip : request.getRemoteAddr();
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

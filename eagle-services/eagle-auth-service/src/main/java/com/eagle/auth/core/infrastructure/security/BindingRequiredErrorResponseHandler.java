package com.eagle.auth.core.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ErrorAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * token 端点错误响应处理器。
 *
 * <p>仅拦截 {@link SocialBindingRequiredException}（{@code binding_required}），
 * 输出附带 {@code bind_ticket} 的 JSON；其余异常委托 SAS 默认的
 * {@link OAuth2ErrorAuthenticationFailureHandler}，不改变现存错误契约。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class BindingRequiredErrorResponseHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    private final AuthenticationFailureHandler delegate =
            new OAuth2ErrorAuthenticationFailureHandler();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        if (!(exception instanceof SocialBindingRequiredException bindingRequired)) {
            delegate.onAuthenticationFailure(request, response, exception);
            return;
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "error", SocialBindingRequiredException.ERROR_CODE,
                "bind_ticket", bindingRequired.getBindTicket(),
                "provider", bindingRequired.getProvider().name())));
    }
}

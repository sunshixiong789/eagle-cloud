package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.SocialBindApplicationService;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.common.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 第三方身份挂靠手机号认证提供者（grant_type = social_bind）。
 *
 * <p>核心逻辑在 {@link SocialBindApplicationService}；此处把业务异常翻译为
 * OAuth2 错误：ticket 失效 → {@code invalid_bind_ticket}（客户端重走第三方授权），
 * 其余 → {@code invalid_grant}。挂接成功后由骨架签发 token，与其他 grant 一致。
 *
 * @author sunshixiong
 */
@Component
public class SocialBindAuthenticationProvider extends AbstractCustomGrantAuthenticationProvider {

    /** ticket 失效专用 OAuth2 error code，客户端据此重走第三方授权而非重试验证码。 */
    public static final String ERROR_INVALID_BIND_TICKET = "invalid_bind_ticket";

    private final SocialBindApplicationService socialBindApplicationService;

    public SocialBindAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            SocialBindApplicationService socialBindApplicationService) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.socialBindApplicationService = socialBindApplicationService;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return SocialBindAuthenticationToken.SOCIAL_BIND;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return SocialBindAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        SocialBindAuthenticationToken token = (SocialBindAuthenticationToken) authentication;
        try {
            return socialBindApplicationService.bind(
                    token.getBindTicket(), token.getPhone(), token.getCode(),
                    ClientIpHolder.get());
        } catch (AppException ex) {
            String errorCode = ex.getErrorCode() == AuthErrorCode.SOCIAL_BIND_TICKET_INVALID
                    ? ERROR_INVALID_BIND_TICKET : "invalid_grant";
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(errorCode, ex.getMessage(), null), ex);
        }
    }
}

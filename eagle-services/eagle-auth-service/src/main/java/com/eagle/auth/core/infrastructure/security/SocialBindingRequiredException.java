package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.domain.model.enums.SocialProvider;
import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.Serial;

/**
 * 第三方身份未挂靠任何账号，需要先绑定手机号。
 *
 * <p>四个第三方 grant 在身份验签成功但未命中绑定时抛出，
 * 由 {@link BindingRequiredErrorResponseHandler} 输出
 * {@code {"error":"binding_required","bind_ticket":…,"provider":…}}，
 * 客户端引导用户走 {@code social_bind} grant 完成挂靠。
 *
 * @author sunshixiong
 */
@Getter
public class SocialBindingRequiredException extends OAuth2AuthenticationException {

    public static final String ERROR_CODE = "binding_required";

    @Serial
    private static final long serialVersionUID = 1L;

    /** 一次性待绑定凭证 ID（Redis，TTL 10 分钟）。 */
    private final String bindTicket;

    private final SocialProvider provider;

    public SocialBindingRequiredException(String bindTicket, SocialProvider provider) {
        super(new OAuth2Error(ERROR_CODE, "第三方身份未绑定手机号，请先完成绑定", null));
        this.bindTicket = bindTicket;
        this.provider = provider;
    }
}

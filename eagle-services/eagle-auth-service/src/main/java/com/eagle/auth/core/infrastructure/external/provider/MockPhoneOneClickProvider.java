package com.eagle.auth.core.infrastructure.external.provider;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.common.util.LogMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Mock 一键登录 Provider：access_token 直接当手机号使用，仅供开发联调
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class MockPhoneOneClickProvider implements PhoneOneClickProvider {

    public static final String NAME = "mock";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String verifyAndGetPhone(String accessToken) {
        if (!PHONE_PATTERN.matcher(accessToken).matches()) {
            log.warn("一键登录 mock 模式收到非法 token，无法解析为手机号");
            throw AuthErrorCode.ONE_CLICK_PHONE_PARSE_FAILED.toDomainException();
        }
        log.warn("一键登录 mock 模式：access_token 直接作为手机号使用，仅供开发环境，phone={}",
                LogMask.phone(accessToken));
        return accessToken;
    }
}

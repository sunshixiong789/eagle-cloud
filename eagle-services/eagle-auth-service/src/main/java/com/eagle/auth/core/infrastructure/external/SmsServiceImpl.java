package com.eagle.auth.core.infrastructure.external;

import com.eagle.common.util.LogMask;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信验证码服务实现。
 *
 * <p>真实短信发送依赖（eagle-notification-starter）已移除，{@link #isConfigured()}
 * 恒返回 {@code false} —— 验证码统一走 {@link AbstractCachedSmsService} 的开发态兜底,
 * 打印到日志而不真实下发,与一键登录 SDK 的下线方式一致。
 *
 * <p>命中 {@link SmsMockProperties} 审核白名单的手机号不打印真实验证码到日志,
 * 直接使用固定验证码校验（App Store 提审用）,其余手机号不受影响。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl extends AbstractCachedSmsService {

    private final SmsMockProperties smsMockProperties;

    @Override
    public void sendCode(String phone) {
        if (smsMockProperties.isMockPhone(phone)) {
            log.warn("审核白名单手机号命中，跳过真实短信发送，使用固定验证码: phone={}", LogMask.phone(phone));
            return;
        }
        super.sendCode(phone);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (smsMockProperties.isMockPhone(phone)) {
            boolean matched = smsMockProperties.getCode().equals(code);
            log.warn("审核白名单手机号使用固定验证码校验: phone={}, matched={}", LogMask.phone(phone), matched);
            return matched;
        }
        return super.verifyCode(phone, code);
    }

    @Override
    protected boolean isConfigured() {
        return false;
    }

    @Override
    protected void doSend(String phone, String code) {
        throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
    }

    @Override
    protected String providerName() {
        return "disabled";
    }
}

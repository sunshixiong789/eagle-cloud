package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import com.eagle.auth.core.infrastructure.config.SmsProperties;
import com.eagle.common.util.LogMask;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信验证码服务实现。
 *
 * <p>真实下发走手拉手 HTTP 网关（{@link HnslsSmsSender}），由 {@code eagle.message.sms}
 * 配置驱动：{@code provider=hnsls} 且账号/密码/签名齐全时真实发送，否则回落到
 * {@link AbstractCachedSmsService} 的开发态兜底——把验证码打进日志而不真实下发，
 * 本地联调无需申请网关账号。
 *
 * <p>命中 {@link SmsMockProperties} 审核白名单的手机号既不发短信也不打验证码日志，
 * 直接用固定验证码校验（App Store 提审用），其余手机号不受影响。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl extends AbstractCachedSmsService {

    private final SmsMockProperties smsMockProperties;
    private final SmsProperties smsProperties;
    private final HnslsSmsSender hnslsSmsSender;

    /**
     * 启动时把当前短信模式打进日志：线上最容易误判的就是"以为配了其实没生效"，
     * 一条启动日志比事后翻发送日志便宜得多。
     */
    @PostConstruct
    void logSmsMode() {
        if (isConfigured()) {
            log.info("短信真实下发已启用: provider={}, sendUrl={}, signName={}",
                    smsProperties.getProvider(), smsProperties.getSendUrl(), smsProperties.getSignName());
        } else {
            log.warn("短信真实下发未启用（provider={}，凭据齐全={}），验证码将打印到日志，仅供开发环境使用",
                    smsProperties.getProvider(), smsProperties.isHnslsCredentialComplete());
        }
    }

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
        return HnslsSmsSender.PROVIDER_NAME.equals(smsProperties.getProvider())
                && smsProperties.isHnslsCredentialComplete();
    }

    @Override
    protected void doSend(String phone, String code) {
        hnslsSmsSender.send(phone, code);
    }

    @Override
    protected String providerName() {
        String provider = smsProperties.getProvider();
        return provider == null || provider.isBlank() ? "disabled" : provider;
    }
}

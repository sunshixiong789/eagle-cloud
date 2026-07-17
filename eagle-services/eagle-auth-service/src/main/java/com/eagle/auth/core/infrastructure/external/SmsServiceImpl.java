package com.eagle.auth.core.infrastructure.external;

import com.eagle.common.util.LogMask;
import com.eagle.message.channel.sms.SmsProvider;
import com.eagle.message.properties.MessageProperties;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.infrastructure.config.SmsMockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 短信验证码服务实现。
 *
 * <p>复用 {@link SmsProvider} 完成实际发送，验证码生成/缓存/限流/校验逻辑由
 * {@link AbstractCachedSmsService} 提供。
 *
 * <p>通过 {@code eagle.message.sms.provider} 切换阿里云/腾讯云/手拉手，无需改动代码。
 * 当未配置有效服务商或签名时，{@link #isConfigured()} 返回 false，验证码仅打印到日志（便于开发联调）。
 *
 * <p>命中 {@link SmsMockProperties} 审核白名单的手机号不发送真实短信，直接使用固定验证码校验
 * （App Store 提审用），其余手机号不受影响。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl extends AbstractCachedSmsService {

    private final ObjectProvider<SmsProvider> smsProviderProvider;
    private final MessageProperties messageProperties;
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
        return smsProviderProvider.getIfAvailable() != null
                && !messageProperties.getSms().getSignName().isBlank();
    }

    @Override
    protected void doSend(String phone, String code) {
        SmsProvider provider = smsProviderProvider.getIfAvailable();
        if (provider == null) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
        }
        try {
            String templateId = messageProperties.getSms().getTemplateId();
            String signName = messageProperties.getSms().getSignName();
            provider.send(phone, templateId, signName, Map.of("code", code));
        } catch (RuntimeException e) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }

    @Override
    protected String providerName() {
        SmsProvider provider = smsProviderProvider.getIfAvailable();
        return provider != null ? provider.name() : "unknown";
    }
}

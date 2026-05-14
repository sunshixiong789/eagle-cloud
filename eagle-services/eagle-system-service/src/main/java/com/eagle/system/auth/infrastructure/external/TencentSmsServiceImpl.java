package com.eagle.system.auth.infrastructure.external;

import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.infrastructure.config.TencentSmsProperties;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 腾讯云短信服务实现。
 *
 * <p>仅当 {@code eagle.sms.provider=tencent} 时装配，与 {@link AliyunSmsServiceImpl} 互斥。
 * 通用验证码缓存/限流/校验逻辑由 {@link AbstractCachedSmsService} 提供。
 *
 * <p>腾讯云手机号必须为 E.164 格式（{@code +86} 前缀），本实现会对 11 位国内号自动补全。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eagle.sms", name = "provider", havingValue = "tencent")
public class TencentSmsServiceImpl extends AbstractCachedSmsService {

    private static final String ENDPOINT = "sms.tencentcloudapi.com";

    private final TencentSmsProperties smsProperties;

    private SmsClient client;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            Credential cred = new Credential(
                    smsProperties.getAccessKeyId(),
                    smsProperties.getAccessKeySecret());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(ENDPOINT);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            this.client = new SmsClient(cred, smsProperties.getRegion(), clientProfile);
        }
    }

    @Override
    protected boolean isConfigured() {
        return !smsProperties.getAccessKeyId().isBlank()
                && !smsProperties.getAccessKeySecret().isBlank()
                && !smsProperties.getSdkAppId().isBlank();
    }

    @Override
    protected String providerName() {
        return "tencent";
    }

    @Override
    protected void doSend(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(smsProperties.getSdkAppId());
            request.setSignName(smsProperties.getSignName());
            request.setTemplateId(smsProperties.getTemplateId());
            request.setPhoneNumberSet(new String[] {normalizePhone(phone)});
            request.setTemplateParamSet(new String[] {code});

            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statuses = response.getSendStatusSet();
            if (statuses == null || statuses.length == 0) {
                log.error("腾讯云短信发送失败：返回为空 phone={}", phone);
                throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
            }
            SendStatus status = statuses[0];
            if (!"Ok".equalsIgnoreCase(status.getCode())) {
                log.error("腾讯云短信发送失败: phone={}, code={}, message={}",
                        phone, status.getCode(), status.getMessage());
                throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }

    /**
     * 国内 11 位手机号补 {@code +86}；已带 {@code +} 或长度不为 11 的原样返回。
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        if (trimmed.length() == 11 && trimmed.startsWith("1")) {
            return "+86" + trimmed;
        }
        return trimmed;
    }
}

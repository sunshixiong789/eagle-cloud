package com.eagle.system.auth.infrastructure.external;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.infrastructure.config.AliyunSmsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信服务实现。
 *
 * <p>仅当 {@code eagle.sms.provider=aliyun}（默认）时装配。
 * 通用验证码缓存/限流/校验逻辑由 {@link AbstractCachedSmsService} 提供。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eagle.sms", name = "provider", havingValue = "aliyun", matchIfMissing = true)
public class AliyunSmsServiceImpl extends AbstractCachedSmsService {

    private final AliyunSmsProperties smsProperties;

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        if (isConfigured()) {
            Config config = new Config()
                    .setAccessKeyId(smsProperties.getAccessKeyId())
                    .setAccessKeySecret(smsProperties.getAccessKeySecret())
                    .setEndpoint("dysmsapi.aliyuncs.com");
            this.client = new Client(config);
        }
    }

    @Override
    protected boolean isConfigured() {
        return !smsProperties.getAccessKeyId().isBlank()
                && !smsProperties.getAccessKeySecret().isBlank();
    }

    @Override
    protected String providerName() {
        return "aliyun";
    }

    @Override
    protected void doSend(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(smsProperties.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse response = client.sendSms(request);
            if (!"OK".equals(response.getBody().getCode())) {
                log.error("阿里云短信发送失败: {}", response.getBody().getMessage());
                throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }
}

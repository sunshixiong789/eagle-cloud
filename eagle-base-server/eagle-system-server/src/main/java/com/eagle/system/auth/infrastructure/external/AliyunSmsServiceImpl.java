package com.eagle.system.auth.infrastructure.external;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.auth.domain.service.SmsService;
import com.eagle.auth.infrastructure.config.AliyunSmsProperties;
import com.eagle.common.exception.codes.AuthErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 阿里云短信服务实现
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsServiceImpl implements SmsService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AliyunSmsProperties smsProperties;

    /**
     * 验证码缓存：phone → code，5分钟过期
     */
    private final Cache<String, String> codeCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10000)
            .build();

    /**
     * 发送频率限制缓存：phone → timestamp，60秒过期
     */
    private final Cache<String, Long> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(10000)
            .build();

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        if (!smsProperties.getAccessKeyId().isBlank() && !smsProperties.getAccessKeySecret().isBlank()) {
            Config config = new Config()
                    .setAccessKeyId(smsProperties.getAccessKeyId())
                    .setAccessKeySecret(smsProperties.getAccessKeySecret())
                    .setEndpoint("dysmsapi.aliyuncs.com");
            this.client = new Client(config);
        }
    }

    @Override
    public void sendCode(String phone) {
        // 频率限制
        if (rateLimitCache.getIfPresent(phone) != null) {
            throw AuthErrorCode.SMS_RATE_LIMIT.toServiceException();
        }

        String code = generateCode();
        codeCache.put(phone, code);
        rateLimitCache.put(phone, System.currentTimeMillis());

        if (client != null) {
            doSend(phone, code);
        } else {
            // 开发环境未配置阿里云时，打印验证码到日志
            log.warn("阿里云短信未配置，验证码: phone={}, code={}", phone, code);
        }
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String cached = codeCache.getIfPresent(phone);
        if (cached != null && cached.equals(code)) {
            codeCache.invalidate(phone);
            return true;
        }
        return false;
    }

    private void doSend(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(smsProperties.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse response = client.sendSms(request);
            if (!"OK".equals(response.getBody().getCode())) {
                log.error("短信发送失败: {}", response.getBody().getMessage());
                throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }
}

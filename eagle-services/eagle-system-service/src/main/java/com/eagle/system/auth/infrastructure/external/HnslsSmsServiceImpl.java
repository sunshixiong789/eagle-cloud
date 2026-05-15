package com.eagle.system.auth.infrastructure.external;

import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.infrastructure.config.HnslsSmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 手拉手 HTTP 短信服务实现。
 *
 * <p>仅当 {@code eagle.sms.provider=hnsls} 时装配。协议要求以表单方式提交
 * {@code name/seed/key/dest/content}，其中 {@code key=md5(md5(password)+seed)}。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "eagle.sms", name = "provider", havingValue = "hnsls")
public class HnslsSmsServiceImpl extends AbstractCachedSmsService {

    private static final DateTimeFormatter SEED_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyyMMddHHmmss");

    private final HnslsSmsProperties smsProperties;
    private final RestClient restClient;
    private final Clock clock;

    @Autowired
    public HnslsSmsServiceImpl(
            HnslsSmsProperties smsProperties,
            RestClient.Builder restClientBuilder) {
        this(smsProperties, restClientBuilder, Clock.systemDefaultZone());
    }

    HnslsSmsServiceImpl(
            HnslsSmsProperties smsProperties,
            RestClient.Builder restClientBuilder,
            Clock clock) {
        this.smsProperties = smsProperties;
        this.restClient = restClientBuilder.build();
        this.clock = clock;
    }

    @Override
    protected boolean isConfigured() {
        return !smsProperties.getUsername().isBlank()
                && !smsProperties.getPassword().isBlank()
                && !smsProperties.getSignName().isBlank()
                && !smsProperties.getSendUrl().isBlank();
    }

    @Override
    protected String providerName() {
        return "hnsls";
    }

    @Override
    protected void doSend(String phone, String code) {
        String seed = LocalDateTime.now(clock).format(SEED_FORMATTER);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name", smsProperties.getUsername());
        formData.add("seed", seed);
        formData.add("key", buildKey(smsProperties.getPassword(), seed));
        formData.add("dest", phone);
        formData.add("content", buildContent(code));

        try {
            String response = restClient.post()
                    .uri(smsProperties.getSendUrl())
                    .contentType(new MediaType(
                            MediaType.APPLICATION_FORM_URLENCODED,
                            Charset.forName(smsProperties.getCharset())))
                    .body(formData)
                    .retrieve()
                    .body(String.class);
            handleResponse(phone, response);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }

    private void handleResponse(String phone, String response) {
        if (response == null || response.isBlank()) {
            log.error("手拉手短信发送失败：返回为空 phone={}", phone);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
        }
        String normalizedResponse = response.trim();
        if (startsWithSuccess(normalizedResponse)) {
            return;
        }
        if (startsWithError(normalizedResponse)) {
            log.error("手拉手短信发送失败: phone={}, response={}",
                    phone, normalizedResponse);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
        }
        log.error("手拉手短信发送失败：未知响应 phone={}, response={}",
                phone, normalizedResponse);
        throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
    }

    private boolean startsWithSuccess(String response) {
        return response.startsWith("success:") || response.startsWith("success：");
    }

    private boolean startsWithError(String response) {
        return response.startsWith("error:") || response.startsWith("error：");
    }

    private String buildContent(String code) {
        return "【" + smsProperties.getSignName() + "】"
                + smsProperties.getContentTemplate().replace("{code}", code);
    }

    private String buildKey(String password, String seed) {
        return md5(md5(password) + seed);
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Charset charset = Charset.forName(smsProperties.getCharset());
            return HexFormat.of().formatHex(digest.digest(value.getBytes(charset)))
                    .toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }
}

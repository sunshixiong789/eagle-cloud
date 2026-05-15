package com.eagle.system.auth.infrastructure.external;

import com.eagle.common.exception.ServiceException;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.infrastructure.config.HnslsSmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

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
    private static final Map<String, String> ERROR_DESCRIPTIONS = Map.ofEntries(
            Map.entry("101", "缺少 name 参数"),
            Map.entry("102", "缺少 seed 参数"),
            Map.entry("103", "缺少 key 参数"),
            Map.entry("104", "缺少 dest 参数"),
            Map.entry("105", "缺少 content 参数"),
            Map.entry(
                    "106",
                    "seed 错误，检查服务器时间/时区是否与网关相差超过 30 分钟"),
            Map.entry("107", "key 错误，检查密码、MD5 算法和编码"),
            Map.entry("109", "内容超长"),
            Map.entry("111", "短信内容缺少签名"),
            Map.entry("113", "签名不合法"),
            Map.entry("201", "无对应账户"),
            Map.entry("202", "账户暂停"),
            Map.entry("204", "账户 IP 未备案"),
            Map.entry("205", "账户无余额"),
            Map.entry("206", "密码错误"),
            Map.entry("305", "无匹配通道"),
            Map.entry("309", "未提供短信服务"),
            Map.entry("401", "短信内容包含屏蔽词"),
            Map.entry("500", "查询间隔太短"),
            Map.entry("999", "其他错误"));

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
        this(
                smsProperties,
                restClientBuilder
                        .requestFactory(createRequestFactory(smsProperties))
                        .build(),
                clock);
    }

    HnslsSmsServiceImpl(
            HnslsSmsProperties smsProperties,
            RestClient restClient,
            Clock clock) {
        this.smsProperties = smsProperties;
        this.restClient = restClient;
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
        String content = buildContent(code);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name", smsProperties.getUsername());
        formData.add("seed", seed);
        formData.add("key", buildKey(smsProperties.getPassword(), seed));
        formData.add("dest", phone);
        formData.add("content", content);

        long start = System.currentTimeMillis();
        try {
            log.debug(
                    "手拉手短信提交: url={}, charset={}, account={}, phone={}, seed={}, "
                            + "signConfigured={}, contentChars={}",
                    smsProperties.getSendUrl(),
                    smsProperties.getCharset(),
                    maskAccount(smsProperties.getUsername()),
                    maskPhone(phone),
                    seed,
                    !smsProperties.getSignName().isBlank(),
                    content.length());
            String response = restClient.post()
                    .uri(smsProperties.getSendUrl())
                    .contentType(new MediaType(
                            MediaType.APPLICATION_FORM_URLENCODED,
                            Charset.forName(smsProperties.getCharset())))
                    .body(formData)
                    .retrieve()
                    .body(String.class);
            handleResponse(phone, response, System.currentTimeMillis() - start);
        } catch (IllegalStateException | ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("手拉手短信提交异常: url={}, phone={}, costMs={}",
                    smsProperties.getSendUrl(), maskPhone(phone),
                    System.currentTimeMillis() - start, e);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(e);
        }
    }

    private void handleResponse(String phone, String response, long costMs) {
        if (response == null || response.isBlank()) {
            log.error("手拉手短信发送失败：返回为空 phone={}, costMs={}",
                    maskPhone(phone), costMs);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
        }
        String normalizedResponse = response.trim();
        log.debug("手拉手短信响应: phone={}, response={}, costMs={}",
                maskPhone(phone), normalizedResponse, costMs);
        if (startsWithSuccess(normalizedResponse)) {
            return;
        }
        if (startsWithError(normalizedResponse)) {
            String errorCode = extractErrorCode(normalizedResponse);
            log.error("手拉手短信发送失败: phone={}, errorCode={}, reason={}, response={}",
                    maskPhone(phone), errorCode, describeError(errorCode), normalizedResponse);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
        }
        log.error("手拉手短信发送失败：未知响应 phone={}, response={}",
                maskPhone(phone), normalizedResponse);
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

    private static SimpleClientHttpRequestFactory createRequestFactory(
            HnslsSmsProperties smsProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(smsProperties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(smsProperties.getReadTimeoutMs()));
        return requestFactory;
    }

    private String extractErrorCode(String response) {
        int separatorIndex = Math.max(response.indexOf(':'), response.indexOf('：'));
        if (separatorIndex < 0 || separatorIndex == response.length() - 1) {
            return "";
        }
        return response.substring(separatorIndex + 1).trim();
    }

    private String describeError(String errorCode) {
        return ERROR_DESCRIPTIONS.getOrDefault(
                errorCode, "未知错误码，请对照手拉手协议附录");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) {
            return "";
        }
        if (account.length() <= 2) {
            return "**";
        }
        return account.charAt(0) + "***" + account.charAt(account.length() - 1);
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

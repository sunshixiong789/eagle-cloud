package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.infrastructure.config.SmsProperties;
import com.eagle.common.util.LogMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * 手拉手 HTTP 短信网关发送器。
 *
 * <p>协议要求以表单方式提交 {@code name/seed/key/dest/content}，其中
 * {@code seed} 为 {@code yyyyMMddHHmmss} 时间戳、{@code key = md5(md5(password) + seed)}。
 * 网关以 {@code success:<流水号>} / {@code error:<错误码>} 的纯文本响应表示结果。
 *
 * <p>本类由 eagle-notification-starter 移除前的 {@code HnslsSmsProvider} 内联而来——
 * auth-service 只需要验证码单场景，不值得为它保留整套多渠道抽象。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
public class HnslsSmsSender {

    public static final String PROVIDER_NAME = "hnsls";

    private static final DateTimeFormatter SEED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 手拉手协议附录的错误码释义，用于把裸错误码翻译成可排查的日志。
     */
    private static final Map<String, String> ERROR_DESCRIPTIONS = Map.ofEntries(
            Map.entry("101", "缺少 name 参数"),
            Map.entry("102", "缺少 seed 参数"),
            Map.entry("103", "缺少 key 参数"),
            Map.entry("104", "缺少 dest 参数"),
            Map.entry("105", "缺少 content 参数"),
            Map.entry("106", "seed 错误，检查服务器时间/时区是否与网关相差超过 30 分钟"),
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

    private final SmsProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    public HnslsSmsSender(SmsProperties properties,
                          @Qualifier("smsRestClient") RestClient restClient) {
        this(properties, restClient, Clock.systemDefaultZone());
    }

    HnslsSmsSender(SmsProperties properties, RestClient restClient, Clock clock) {
        this.properties = properties;
        this.restClient = restClient;
        this.clock = clock;
    }

    /**
     * 下发验证码短信，失败时抛 {@link AuthErrorCode#SMS_SEND_FAILED}。
     *
     * @param phone 接收手机号
     * @param code  6 位验证码
     */
    public void send(String phone, String code) {
        String seed = LocalDateTime.now(clock).format(SEED_FORMATTER);
        String content = buildContent(code);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name", properties.getUsername());
        formData.add("seed", seed);
        formData.add("key", buildKey(properties.getPassword(), seed));
        formData.add("dest", phone);
        formData.add("content", content);

        long start = System.currentTimeMillis();
        String maskedPhone = LogMask.phone(phone);
        String response;
        try {
            log.debug("hnsls sms submit: url={}, charset={}, account={}, phone={}, contentLen={}",
                    properties.getSendUrl(), properties.getCharset(),
                    maskAccount(properties.getUsername()), maskedPhone, content.length());
            response = restClient.post()
                    .uri(properties.getSendUrl())
                    .contentType(new MediaType(MediaType.APPLICATION_FORM_URLENCODED,
                            Charset.forName(properties.getCharset())))
                    .body(formData)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.error("hnsls sms submit error: url={}, phone={}, costMs={}",
                    properties.getSendUrl(), maskedPhone, System.currentTimeMillis() - start, ex);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(ex);
        }
        handleResponse(maskedPhone, response, System.currentTimeMillis() - start);
    }

    /**
     * 解析网关纯文本响应，非 {@code success:} 前缀一律视为失败。
     */
    private void handleResponse(String maskedPhone, String response, long costMs) {
        if (response == null || response.isBlank()) {
            log.error("hnsls sms failed: empty response, url={}, phone={}, costMs={}",
                    properties.getSendUrl(), maskedPhone, costMs);
            throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
        }

        String normalized = response.trim();
        if (normalized.startsWith("success:") || normalized.startsWith("success：")) {
            log.info("hnsls sms sent: phone={}, costMs={}", maskedPhone, costMs);
            return;
        }

        if (normalized.startsWith("error:") || normalized.startsWith("error：")) {
            String errorCode = extractErrorCode(normalized);
            log.error("hnsls sms failed: url={}, phone={}, costMs={}, errorCode={}, reason={}",
                    properties.getSendUrl(), maskedPhone, costMs, errorCode, describeError(errorCode));
        } else {
            // 响应体形态未知，可能夹带账号信息，只记长度不记内容
            log.error("hnsls sms failed: unknown response, url={}, phone={}, costMs={}, length={}",
                    properties.getSendUrl(), maskedPhone, costMs, normalized.length());
        }
        throw AuthErrorCode.SMS_SEND_FAILED.toServiceException();
    }

    private String buildContent(String code) {
        return "【" + properties.getSignName() + "】"
                + properties.getContentTemplate().replace("{code}", code);
    }

    private String buildKey(String password, String seed) {
        return md5(md5(password) + seed);
    }

    private String extractErrorCode(String response) {
        int sep = Math.max(response.indexOf(':'), response.indexOf('：'));
        if (sep < 0 || sep == response.length() - 1) {
            return "";
        }
        return response.substring(sep + 1).trim();
    }

    private String describeError(String errorCode) {
        return ERROR_DESCRIPTIONS.getOrDefault(errorCode, "未知错误码，请对照手拉手协议附录");
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
            Charset charset = Charset.forName(properties.getCharset());
            return HexFormat.of().formatHex(digest.digest(value.getBytes(charset)))
                    .toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm not available", ex);
        }
    }
}

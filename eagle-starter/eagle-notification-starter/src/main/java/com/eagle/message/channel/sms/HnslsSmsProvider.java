package com.eagle.message.channel.sms;

import com.eagle.message.properties.MessageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
 * 手拉手 HTTP 短信服务商实现。
 *
 * <p>协议要求以表单方式提交 {@code name/seed/key/dest/content}，
 * 其中 {@code key=md5(md5(password)+seed)}。
 *
 * <p>仅当 {@code eagle.message.sms.provider=hnsls} 时装配。
 *
 * @author eagle
 */
@Slf4j
public class HnslsSmsProvider implements SmsProvider {

    public static final String NAME = "hnsls";

    private static final DateTimeFormatter SEED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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

    private final MessageProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    public HnslsSmsProvider(MessageProperties properties) {
        this(properties, RestClient.builder().requestFactory(createRequestFactory(properties)),
                Clock.systemDefaultZone());
    }

    HnslsSmsProvider(MessageProperties properties, RestClient.Builder builder, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.restClient = builder.build();
    }

    private static SimpleClientHttpRequestFactory createRequestFactory(MessageProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getSms().getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getSms().getReadTimeoutMs()));
        return factory;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void send(String phone, String templateId, String signName, Map<String, String> params) {
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Hnsls SMS requires 'code' parameter");
        }

        String seed = LocalDateTime.now(clock).format(SEED_FORMATTER);
        String content = buildContent(signName, code);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name", properties.getSms().getUsername());
        formData.add("seed", seed);
        formData.add("key", buildKey(properties.getSms().getPassword(), seed));
        formData.add("dest", phone);
        formData.add("content", content);

        long start = System.currentTimeMillis();
        String maskedPhone = maskPhone(phone);
        try {
            log.debug("Hnsls SMS submit: url={}, charset={}, account={}, phone={}, contentLen={}",
                    properties.getSms().getSendUrl(),
                    properties.getSms().getCharset(),
                    maskAccount(properties.getSms().getUsername()),
                    maskedPhone,
                    content.length());
            String response = restClient.post()
                    .uri(properties.getSms().getSendUrl())
                    .contentType(new MediaType(
                            MediaType.APPLICATION_FORM_URLENCODED,
                            Charset.forName(properties.getSms().getCharset())))
                    .body(formData)
                    .retrieve()
                    .body(String.class);
            handleResponse(maskedPhone, response, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Hnsls SMS submit error: url={}, phone={}, costMs={}",
                    properties.getSms().getSendUrl(),
                    maskedPhone,
                    System.currentTimeMillis() - start, e);
            throw new RuntimeException("Hnsls SMS send failed", e);
        }
    }

    private void handleResponse(String maskedPhone, String response, long costMs) {
        if (response == null || response.isBlank()) {
            log.error("Hnsls SMS failed: empty response, url={}, phone={}, costMs={}",
                    properties.getSms().getSendUrl(), maskedPhone, costMs);
            throw new RuntimeException("Hnsls SMS empty response");
        }
        String normalized = response.trim();
        if (normalized.startsWith("success:") || normalized.startsWith("success：")) {
            log.debug("Hnsls SMS success: phone={}, costMs={}", maskedPhone, costMs);
            return;
        }
        String errorCode = "";
        String reason;
        if (normalized.startsWith("error:") || normalized.startsWith("error：")) {
            errorCode = extractErrorCode(normalized);
            reason = describeError(errorCode);
            log.error("Hnsls SMS failed: url={}, phone={}, costMs={}, errorCode={}, reason={}",
                    properties.getSms().getSendUrl(), maskedPhone, costMs, errorCode, reason);
        } else {
            reason = "unexpected response prefix";
            log.error("Hnsls SMS failed: unknown response, url={}, phone={}, costMs={}, length={}",
                    properties.getSms().getSendUrl(), maskedPhone, costMs, normalized.length());
        }
        throw new RuntimeException("Hnsls SMS error: " + reason);
    }

    private String buildContent(String signName, String code) {
        String template = properties.getSms().getContentTemplate();
        return "【" + signName + "】" + template.replace("{code}", code);
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
            Charset charset = Charset.forName(properties.getSms().getCharset());
            return HexFormat.of().formatHex(digest.digest(value.getBytes(charset)))
                    .toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}

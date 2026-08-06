package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.TaobaoService;
import com.eagle.auth.core.infrastructure.config.TaobaoAppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link TaobaoService} 的手写 RestClient 实现，直调淘宝开放平台 TOP 网关。
 *
 * <p>主路径（百川 native 一键授权）：access token 作为 TOP session 调 {@code taobao.openuid.get}
 * 直取 {@code open_uid}。兜底路径（授权码）：调 {@code taobao.top.auth.token.create} 用授权码换
 * {@code token_result}（JSON 字符串），从中取 {@code taobao_open_uid}。两者都得到稳定身份标识。
 *
 * <p>只用到 TOP 的这两个接口，不值得为此引入无 Maven 坐标的官方 SDK 本地 jar
 * （与 {@link WechatMiniProgramServiceImpl} 处理微信小程序登录同一套思路）。签名算法
 * （系统参数排序拼接 + secret 首尾包裹 + md5/hmac/hmac-sha256）按 TOP 公开协议实现。
 *
 * <p>{@code eagle.taobao.app.enabled=false} 时直接抛 {@code TAOBAO_UPSTREAM}。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
public class TaobaoServiceImpl implements TaobaoService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId TOP_ZONE = ZoneId.of("Asia/Shanghai");

    private final TaobaoAppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public TaobaoServiceImpl(TaobaoAppProperties properties,
                              @Qualifier("taobaoRestClient") RestClient restClient,
                              ObjectMapper objectMapper) {
        this(properties, restClient, objectMapper, Clock.system(TOP_ZONE));
    }

    /** 测试用构造器：允许注入固定 Clock。 */
    TaobaoServiceImpl(TaobaoAppProperties properties, RestClient restClient,
                       ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String resolveOpenUid(String tbAccessToken, String tbAuthCode) {
        boolean hasToken = tbAccessToken != null && !tbAccessToken.isBlank();
        boolean hasCode = tbAuthCode != null && !tbAuthCode.isBlank();
        if (!hasToken && !hasCode) {
            throw AuthErrorCode.TAOBAO_AUTH_REQUIRED.toDomainException();
        }
        if (!properties.isEnabled()) {
            log.warn("淘宝登录未启用（eagle.taobao.app.enabled=false）");
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        // 百川 native SDK 授权返回的是 access token（非授权码），优先用它凭 session 直取 openUid；
        // tb_auth_code 路径保留作兜底（将来 Web/H5 授权码流可复用）。
        return hasToken
                ? resolveByAccessToken(tbAccessToken)
                : resolveByAuthCode(tbAuthCode);
    }

    /**
     * 主路径：access token 作为 TOP session 调 {@code taobao.openuid.get} 直取 openUid。
     */
    private String resolveByAccessToken(String accessToken) {
        JsonNode payload = callTopApi("taobao.openuid.get", Map.of(), accessToken);
        String openUid = trimToNull(payload.path("open_uid").asString(null));
        if (openUid == null) {
            log.warn("TOP openuid.get 未返回 open_uid");
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        return openUid;
    }

    /**
     * 兜底路径：用 TOP 授权码调 {@code taobao.top.auth.token.create} 换 token_result，
     * 从中取 taobao_open_uid。
     */
    private String resolveByAuthCode(String topAuthCode) {
        JsonNode payload = callTopApi(
                "taobao.top.auth.token.create", Map.of("code", topAuthCode), null);
        String tokenResult = payload.path("token_result").asString(null);
        if (tokenResult == null || tokenResult.isBlank()) {
            log.warn("TOP top.auth.token.create 未返回 token_result");
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        return parseOpenUid(tokenResult);
    }

    /**
     * 调 TOP 网关，返回业务负载节点（成功负载或 {@code error_response} 负载）；
     * 上游不可达或业务失败统一抛 {@code TAOBAO_UPSTREAM}。
     */
    private JsonNode callTopApi(String method, Map<String, String> bizParams, String session) {
        SortedMap<String, String> params = new TreeMap<>();
        params.put("method", method);
        params.put("v", "2.0");
        params.put("format", "json");
        params.put("app_key", properties.getAppKey());
        params.put("timestamp", TIMESTAMP_FORMATTER.format(ZonedDateTime.now(clock)));
        params.put("sign_method", properties.getSignMethod());
        if (session != null && !session.isBlank()) {
            params.put("session", session);
        }
        params.putAll(bizParams);
        params.put("sign", sign(params, properties.getAppSecret(), properties.getSignMethod()));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        String body;
        try {
            body = restClient.post()
                    .uri(properties.getServerUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.warn("TOP {} 调用异常", method, ex);
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException(ex);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JacksonException ex) {
            log.warn("TOP {} 响应解析失败", method, ex);
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException(ex);
        }

        // TOP 响应固定只有一个顶层字段（成功是 {method}_response，失败是 error_response），
        // 取其唯一取值即为业务负载，成功/失败字段（code/sub_code）都挂在这层。
        JsonNode payload = root == null ? null : root.properties().stream()
                .findFirst()
                .map(Entry::getValue)
                .orElse(null);
        if (payload == null || !isTopSuccess(payload)) {
            log.warn("TOP {} 失败: code={}, subCode={}, subMsg={}", method,
                    payload == null ? null : payload.path("code").asString(null),
                    payload == null ? null : payload.path("sub_code").asString(null),
                    payload == null ? null : payload.path("sub_msg").asString(null));
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        return payload;
    }

    private static boolean isTopSuccess(JsonNode payload) {
        String code = trimToNull(payload.path("code").asString(null));
        String subCode = trimToNull(payload.path("sub_code").asString(null));
        return (code == null || "0".equals(code)) && subCode == null;
    }

    private String parseOpenUid(String tokenResult) {
        try {
            JsonNode node = objectMapper.readTree(tokenResult);
            String openUid = node.path("taobao_open_uid").asString(null);
            if (openUid == null || openUid.isBlank()) {
                // 部分应用资质无 open_uid，退回 taobao_user_id 作为标识
                openUid = node.path("taobao_user_id").asString(null);
            }
            if (openUid == null || openUid.isBlank()) {
                // token_result 含明文 access_token / refresh_token，禁止整体落日志（13-logging.md）；
                // 仅输出字段名用于排查。
                log.warn("token_result 未含 taobao_open_uid/taobao_user_id, fields={}",
                        fieldNames(node));
                throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
            }
            return openUid;
        } catch (JacksonException ex) {
            // 不输出原始 tokenResult（可能含明文 token）；仅记录长度与异常。
            log.warn("解析 token_result 失败, length={}", tokenResult == null ? 0 : tokenResult.length(), ex);
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException(ex);
        }
    }

    /**
     * 提取 JSON 字段名列表（不含值），用于安全排查——避免把含明文 token 的值写进日志。
     */
    private static String fieldNames(JsonNode node) {
        return String.join(",", node.propertyNames());
    }

    /**
     * TOP 请求签名：按 key 升序拼接 {@code key+value}（跳过空 key/空 value），
     * md5 首尾包裹 secret 后取 MD5；hmac/hmac-sha256 以 secret 为密钥对拼接串取 HMAC。
     * 结果统一大写十六进制。
     */
    private static String sign(SortedMap<String, String> params, String secret, String signMethod) {
        StringBuilder message = new StringBuilder();
        if ("md5".equals(signMethod)) {
            message.append(secret);
        }
        for (Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                message.append(key).append(value);
            }
        }
        byte[] digest = switch (signMethod) {
            case "hmac" -> hmac(message.toString(), secret, "HmacMD5");
            case "hmac-sha256" -> hmac(message.toString(), secret, "HmacSHA256");
            default -> md5(message.append(secret).toString());
        };
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    private static byte[] hmac(String message, String secret, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOP 签名失败", ex);
        }
    }

    private static byte[] md5(String message) {
        try {
            return MessageDigest.getInstance("MD5").digest(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOP 签名失败", ex);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

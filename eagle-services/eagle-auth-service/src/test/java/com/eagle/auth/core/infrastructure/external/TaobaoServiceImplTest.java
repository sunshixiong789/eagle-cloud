package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.infrastructure.config.TaobaoAppProperties;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("淘宝 TOP 服务实现")
class TaobaoServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-06T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("resolveOpenUid")
    class ResolveOpenUid {

        @Test
        @DisplayName("access token 与 auth code 均为空 → 抛 DomainException")
        void rejectsWhenBothBlank() {
            TaobaoServiceImpl service = newService(properties());

            assertThrows(DomainException.class, () -> service.resolveOpenUid(null, "  "));
        }

        @Test
        @DisplayName("功能未启用 → 抛 ServiceException，且不发起上游调用")
        void rejectsWhenDisabled() {
            TaobaoAppProperties properties = properties();
            properties.setEnabled(false);
            TaobaoServiceImpl service = newService(properties);

            assertThrows(ServiceException.class,
                    () -> service.resolveOpenUid("acc-token", null));
        }

        @Test
        @DisplayName("有 access token → 用 taobao.openuid.get 凭 session 签名取 openUid（百川一键授权主路径）")
        void resolvesOpenUidByAccessToken() {
            TaobaoAppProperties properties = properties();
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            TaobaoServiceImpl service = new TaobaoServiceImpl(
                    properties, builder.build(), OBJECT_MAPPER, FIXED_CLOCK);

            server.expect(requestTo(properties.getServerUrl()))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(signedTopRequest("taobao.openuid.get", properties,
                            Map.of("session", "acc-token")))
                    .andRespond(withSuccess(
                            "{\"openuid_get_response\":{\"open_uid\":\"open-uid-123\"}}",
                            MediaType.APPLICATION_JSON));

            String openUid = service.resolveOpenUid("acc-token", null);

            assertEquals("open-uid-123", openUid);
            server.verify();
        }

        @Test
        @DisplayName("openuid.get 上游返回 sub_code → 抛 ServiceException")
        void wrapsUpstreamFailure() {
            TaobaoAppProperties properties = properties();
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            TaobaoServiceImpl service = new TaobaoServiceImpl(
                    properties, builder.build(), OBJECT_MAPPER, FIXED_CLOCK);

            server.expect(requestTo(properties.getServerUrl()))
                    .andRespond(withSuccess(
                            "{\"error_response\":{\"code\":\"15\","
                                    + "\"sub_code\":\"isv.invalid-session\","
                                    + "\"sub_msg\":\"会话已过期\"}}",
                            MediaType.APPLICATION_JSON));

            assertThrows(ServiceException.class,
                    () -> service.resolveOpenUid("bad-token", null));
            server.verify();
        }

        @Test
        @DisplayName("无 access token 但有 auth code → 用 taobao.top.auth.token.create 换 token_result 兜底取 openUid")
        void resolvesOpenUidByAuthCode() {
            TaobaoAppProperties properties = properties();
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            TaobaoServiceImpl service = new TaobaoServiceImpl(
                    properties, builder.build(), OBJECT_MAPPER, FIXED_CLOCK);

            server.expect(requestTo(properties.getServerUrl()))
                    .andExpect(signedTopRequest("taobao.top.auth.token.create", properties,
                            Map.of("code", "auth-code-1")))
                    .andRespond(withSuccess(
                            "{\"top_auth_token_create_response\":{\"token_result\":"
                                    + "\"{\\\"taobao_open_uid\\\":\\\"open-uid-456\\\"}\"}}",
                            MediaType.APPLICATION_JSON));

            String openUid = service.resolveOpenUid(null, "auth-code-1");

            assertEquals("open-uid-456", openUid);
            server.verify();
        }
    }

    private static TaobaoServiceImpl newService(TaobaoAppProperties properties) {
        return new TaobaoServiceImpl(
                properties, RestClient.builder().build(), OBJECT_MAPPER, FIXED_CLOCK);
    }

    private static TaobaoAppProperties properties() {
        TaobaoAppProperties properties = new TaobaoAppProperties();
        properties.setEnabled(true);
        properties.setServerUrl("https://eco.taobao.com/router/rest");
        properties.setAppKey("test-app-key");
        properties.setAppSecret("test-app-secret");
        properties.setSignMethod("md5");
        return properties;
    }

    /**
     * 校验请求体中的固定系统参数、业务参数，并独立重算签名与请求中的 {@code sign} 比对——
     * 不复用生产代码的签名实现，避免测试和实现同时错还互相掩盖。
     */
    private static RequestMatcher signedTopRequest(
            String method, TaobaoAppProperties properties, Map<String, String> expectedBizParams) {
        return request -> {
            String body = ((MockClientHttpRequest) request).getBodyAsString();
            Map<String, String> received = parseForm(body);

            assertEquals(method, received.get("method"));
            assertEquals("2.0", received.get("v"));
            assertEquals("json", received.get("format"));
            assertEquals(properties.getAppKey(), received.get("app_key"));
            assertEquals(properties.getSignMethod(), received.get("sign_method"));
            expectedBizParams.forEach((key, value) -> assertEquals(value, received.get(key)));
            assertThat(received.get("timestamp")).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

            SortedMap<String, String> signedParams = new TreeMap<>(received);
            signedParams.remove("sign");
            String expectedSign = referenceMd5Sign(signedParams, properties.getAppSecret());
            assertEquals(expectedSign, received.get("sign"));
        };
    }

    /** TOP md5 签名算法的独立参照实现：md5(secret + 升序key+value拼接 + secret) 转大写十六进制。 */
    private static String referenceMd5Sign(SortedMap<String, String> params, String secret) {
        StringBuilder message = new StringBuilder(secret);
        params.forEach((key, value) -> {
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                message.append(key).append(value);
            }
        });
        message.append(secret);
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(message.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> params = new TreeMap<>();
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }
}

package com.eagle.system.auth.infrastructure.external;

import com.eagle.system.auth.infrastructure.config.HnslsSmsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 手拉手短信服务测试。
 *
 * @author sunshixiong
 */
class HnslsSmsServiceImplTest {

    private static final String SEND_URL = "https://xapi.hnsls.com.cn/eums/sms/utf8/send.do";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2013-08-06T02:20:30Z"),
            ZoneId.of("Asia/Shanghai"));

    @Nested
    @DisplayName("doSend")
    class DoSend {

        @Test
        @DisplayName("should submit form request when gateway returns success")
        void shouldSubmitFormRequestWhenGatewayReturnsSuccess() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            HnslsSmsServiceImpl service = new HnslsSmsServiceImpl(
                    properties(),
                    builder,
                    FIXED_CLOCK);

            server.expect(requestTo(SEND_URL))
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(content().string(
                            "name=test&seed=20130806102030&key="
                                    + "cd6e1aa6b89e8e413867b33811e70153"
                                    + "&dest=13800138000"
                                    + "&content=%E3%80%90%E9%B9%B0%E4%BA%91%E3%80%91"
                                    + "%E6%82%A8%E7%9A%84%E9%AA%8C%E8%AF%81%E7%A0%81%E6%98%AF123456"
                                    + "%EF%BC%8C5%E5%88%86%E9%92%9F%E5%86%85"
                                    + "%E6%9C%89%E6%95%88%E3%80%82"))
                    .andRespond(withSuccess("success:0603015522454756885", MediaType.TEXT_PLAIN));

            service.doSend("13800138000", "123456");

            server.verify();
        }

        @Test
        @DisplayName("should throw service exception when gateway returns error")
        void shouldThrowServiceExceptionWhenGatewayReturnsError() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            HnslsSmsServiceImpl service = new HnslsSmsServiceImpl(
                    properties(),
                    builder,
                    FIXED_CLOCK);

            server.expect(requestTo(SEND_URL))
                    .andRespond(withSuccess("error:206", MediaType.TEXT_PLAIN));

            assertThrows(RuntimeException.class, () -> service.doSend("13800138000", "123456"));
            server.verify();
        }
    }

    @Nested
    @DisplayName("isConfigured")
    class IsConfigured {

        @Test
        @DisplayName("should return false when username is blank")
        void shouldReturnFalseWhenUsernameIsBlank() {
            HnslsSmsProperties properties = properties();
            properties.setUsername("");

            HnslsSmsServiceImpl service = new HnslsSmsServiceImpl(
                    properties,
                    RestClient.builder(),
                    FIXED_CLOCK);

            assertFalse(service.isConfigured());
        }
    }

    private HnslsSmsProperties properties() {
        HnslsSmsProperties properties = new HnslsSmsProperties();
        properties.setUsername("test");
        properties.setPassword("123456");
        properties.setSignName("鹰云");
        properties.setContentTemplate("您的验证码是{code}，5分钟内有效。");
        properties.setSendUrl(SEND_URL);
        properties.setCharset(StandardCharsets.UTF_8.name());
        return properties;
    }
}

package com.eagle.message.channel.sms;

import com.eagle.message.properties.MessageProperties;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 手拉手短信服务商测试。
 *
 * @author sunshixiong
 */
class HnslsSmsProviderTest {

    private static final String SEND_URL = "https://xapi.hnsls.com.cn/eums/sms/utf8/send.do";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2013-08-06T02:20:30Z"),
            ZoneId.of("Asia/Shanghai"));

    private MessageProperties properties() {
        MessageProperties properties = new MessageProperties();
        MessageProperties.Sms sms = properties.getSms();
        sms.setProvider(HnslsSmsProvider.NAME);
        sms.setUsername("test");
        sms.setPassword("123456");
        sms.setSignName("鹰云");
        sms.setContentTemplate("您的验证码是{code}，5分钟内有效。");
        sms.setSendUrl(SEND_URL);
        sms.setCharset(StandardCharsets.UTF_8.name());
        return properties;
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("网关返回成功时应提交表单请求")
        void shouldSubmitFormRequestWhenGatewayReturnsSuccess() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            HnslsSmsProvider provider = new HnslsSmsProvider(properties(), builder, FIXED_CLOCK);

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

            provider.send("13800138000", "ignored", "鹰云", Map.of("code", "123456"));

            server.verify();
        }

        @Test
        @DisplayName("网关返回错误时应抛出")
        void shouldThrowWhenGatewayReturnsError() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            HnslsSmsProvider provider = new HnslsSmsProvider(properties(), builder, FIXED_CLOCK);

            server.expect(requestTo(SEND_URL))
                    .andRespond(withSuccess("error:206", MediaType.TEXT_PLAIN));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> provider.send("13800138000", "ignored", "鹰云", Map.of("code", "123456")));
            // 错误描述应根据手拉手协议附录解析，避免泄露原始响应
            assertEquals("Hnsls SMS error: 密码错误", ex.getMessage());
            server.verify();
        }

        @Test
        @DisplayName("网关返回空时应抛出")
        void shouldThrowWhenGatewayReturnsEmpty() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            HnslsSmsProvider provider = new HnslsSmsProvider(properties(), builder, FIXED_CLOCK);

            server.expect(requestTo(SEND_URL))
                    .andRespond(withSuccess("", MediaType.TEXT_PLAIN));

            assertThrows(RuntimeException.class,
                    () -> provider.send("13800138000", "ignored", "鹰云", Map.of("code", "123456")));
            server.verify();
        }

        @Test
        @DisplayName("应拒绝缺失验证码")
        void shouldRejectMissingCode() {
            HnslsSmsProvider provider = new HnslsSmsProvider(
                    properties(), RestClient.builder(), FIXED_CLOCK);

            assertThrows(IllegalArgumentException.class,
                    () -> provider.send("13800138000", "ignored", "鹰云", Map.of()));
        }
    }

    @Nested
    @DisplayName("name")
    class Name {

        @Test
        @DisplayName("应返回标识")
        void shouldReturnIdentifier() {
            HnslsSmsProvider provider = new HnslsSmsProvider(
                    properties(), RestClient.builder(), FIXED_CLOCK);
            assertEquals(HnslsSmsProvider.NAME, provider.name());
        }
    }
}

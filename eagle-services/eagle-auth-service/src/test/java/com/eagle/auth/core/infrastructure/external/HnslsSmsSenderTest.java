package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.infrastructure.config.SmsProperties;
import com.eagle.common.exception.ServiceException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

/**
 * 手拉手短信网关发送器测试。
 *
 * @author sunshixiong
 */
@DisplayName("HnslsSmsSender")
class HnslsSmsSenderTest {

    private static final String SEND_URL = "https://xapi.hnsls.com.cn/eums/sms/utf8/send.do";
    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";

    /**
     * 固定时钟：seed 参与 key 计算，必须固定才能断言签名结果。
     */
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2013-08-06T02:20:30Z"), ZoneId.of("Asia/Shanghai"));

    private SmsProperties properties() {
        SmsProperties properties = new SmsProperties();
        properties.setProvider(HnslsSmsSender.PROVIDER_NAME);
        properties.setUsername("test");
        properties.setPassword("123456");
        properties.setSignName("鹰云");
        properties.setContentTemplate("您的验证码是{code}，5分钟内有效。");
        properties.setSendUrl(SEND_URL);
        properties.setCharset(StandardCharsets.UTF_8.name());
        return properties;
    }

    private record Fixture(HnslsSmsSender sender, MockRestServiceServer server) {
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new HnslsSmsSender(properties(), builder.build(), FIXED_CLOCK), server);
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("网关返回成功时按协议提交表单，key=md5(md5(password)+seed)")
        void shouldSubmitFormWhenGatewayReturnsSuccess() {
            Fixture fixture = fixture();

            fixture.server().expect(requestTo(SEND_URL))
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

            fixture.sender().send(PHONE, CODE);

            fixture.server().verify();
        }

        @Test
        @DisplayName("网关返回错误码时抛 SMS_SEND_FAILED")
        void shouldThrowWhenGatewayReturnsError() {
            Fixture fixture = fixture();
            fixture.server().expect(requestTo(SEND_URL))
                    .andRespond(withSuccess("error:206", MediaType.TEXT_PLAIN));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> fixture.sender().send(PHONE, CODE));

            assertEquals(AuthErrorCode.SMS_SEND_FAILED, ex.getErrorCode());
            fixture.server().verify();
        }

        @Test
        @DisplayName("网关返回空响应时抛 SMS_SEND_FAILED")
        void shouldThrowWhenGatewayReturnsEmpty() {
            Fixture fixture = fixture();
            fixture.server().expect(requestTo(SEND_URL))
                    .andRespond(withSuccess("", MediaType.TEXT_PLAIN));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> fixture.sender().send(PHONE, CODE));

            assertEquals(AuthErrorCode.SMS_SEND_FAILED, ex.getErrorCode());
            fixture.server().verify();
        }

        @Test
        @DisplayName("响应前缀未知时抛 SMS_SEND_FAILED")
        void shouldThrowWhenResponsePrefixUnknown() {
            Fixture fixture = fixture();
            fixture.server().expect(requestTo(SEND_URL))
                    .andRespond(withSuccess("<html>maintenance</html>", MediaType.TEXT_PLAIN));

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> fixture.sender().send(PHONE, CODE));

            assertEquals(AuthErrorCode.SMS_SEND_FAILED, ex.getErrorCode());
            fixture.server().verify();
        }

        @Test
        @DisplayName("网关 HTTP 异常时包装为 SMS_SEND_FAILED，不泄漏底层异常")
        void shouldWrapTransportError() {
            Fixture fixture = fixture();
            fixture.server().expect(requestTo(SEND_URL)).andRespond(withServerError());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> fixture.sender().send(PHONE, CODE));

            assertEquals(AuthErrorCode.SMS_SEND_FAILED, ex.getErrorCode());
            fixture.server().verify();
        }
    }
}

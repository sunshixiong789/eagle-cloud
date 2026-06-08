package com.eagle.http.client.interceptor;

import com.eagle.common.pressuretest.PressureTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link PropagatingHeadersClientHttpRequestInterceptor} 单元测试。
 *
 * @author eagle
 */
class PropagatingHeadersClientHttpRequestInterceptorTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        PressureTestContext.clear();
    }

    @Test
    @DisplayName("应透传认证、语言和压测请求头")
    void shouldPropagateAuthLanguageAndPressureTestHeaders() {
        MockHttpServletRequest inbound = new MockHttpServletRequest();
        inbound.addHeader("Authorization", "Bearer token");
        inbound.addHeader("Accept-Language", "zh-CN");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(inbound));
        PressureTestContext.mark();

        RestClient.Builder builder = RestClient.builder()
                .requestInterceptor(new PropagatingHeadersClientHttpRequestInterceptor());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/downstream"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(header("Accept-Language", "zh-CN"))
                .andExpect(header("X-Eagle-Gray", "true"))
                .andRespond(withSuccess());

        builder.build().get().uri("/downstream").retrieve().toBodilessEntity();

        server.verify();
    }
}

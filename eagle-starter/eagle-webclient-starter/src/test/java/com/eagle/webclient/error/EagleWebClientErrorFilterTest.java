package com.eagle.webclient.error;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EagleWebClientErrorFilter} 单元测试。
 *
 * @author 孙士雄
 */
class EagleWebClientErrorFilterTest {

    private final EagleWebClientErrorFilter filter = new EagleWebClientErrorFilter(new ObjectMapper());

    private ClientRequest dummyRequest() {
        return ClientRequest.create(HttpMethod.GET, URI.create("http://example/api/users/99")).build();
    }

    private ExchangeFunction stubResponse(int status, String body) {
        return req -> Mono.just(ClientResponse.create(HttpStatus.valueOf(status))
                .header("Content-Type", "application/json")
                .body(body == null ? "" : body)
                .build());
    }

    @Test
    @DisplayName("404 → NotFoundException，message 从 JSON 提取到 args")
    void shouldRaiseNotFoundOn404() {
        Mono<ClientResponse> mono = filter.filter(dummyRequest(),
                stubResponse(404, "{\"status\":404,\"message\":\"用户不存在\",\"errorCode\":10001}"));

        assertThatThrownBy(mono::block)
                .isInstanceOf(NotFoundException.class)
                .satisfies(t -> assertThat(((NotFoundException) t).getMessageArgs()[0])
                        .isEqualTo("用户不存在"));
    }

    @Test
    @DisplayName("409 → ConflictException")
    void shouldRaiseConflictOn409() {
        Mono<ClientResponse> mono = filter.filter(dummyRequest(),
                stubResponse(409, "{\"status\":409,\"message\":\"订单已支付\"}"));

        assertThatThrownBy(mono::block).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("400 → DomainException")
    void shouldRaiseDomainOn400() {
        Mono<ClientResponse> mono = filter.filter(dummyRequest(),
                stubResponse(400, "{\"status\":400,\"message\":\"参数非法\"}"));

        assertThatThrownBy(mono::block).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("500 → ServiceException")
    void shouldRaiseServiceOn500() {
        Mono<ClientResponse> mono = filter.filter(dummyRequest(), stubResponse(500, "internal error"));

        assertThatThrownBy(mono::block).isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("2xx 响应保持原样穿过")
    void shouldPassThroughSuccessResponse() {
        Mono<ClientResponse> mono = filter.filter(dummyRequest(), stubResponse(200, "ok"));

        ClientResponse response = mono.block();
        assertThat(response).isNotNull();
        assertThat(response.statusCode().value()).isEqualTo(200);
    }
}

package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.infrastructure.event.AuthLoginLogPublisher;
import com.eagle.auth.core.infrastructure.event.LoginLogIntegrationEvent;
import com.eagle.common.dto.EagleUser;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthenticationEventListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private DomainEventPublisher publisher;
    @InjectMocks
    private AuthenticationEventListener listener;

    @AfterEach
    void clearClientContext() {
        ClientIpHolder.clear();
    }

    @Nested
    @DisplayName("onAuthSuccess")
    class OnAuthSuccess {

        @Test
        @DisplayName("用户登录成功(UsernamePasswordAuthenticationToken)应发布登录成功日志事件")
        void shouldPublishLoginSuccessEvent() {
            ClientIpHolder.set("203.0.113.10");
            EagleUser user = new EagleUser(1L, "alice", "$2a$10$xx", "Alice", "13800138000",
                    null, true, true, true, true, Collections.emptyList());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            listener.onAuthSuccess(new AuthenticationSuccessEvent(auth));

            verify(loginAttemptService).registerSuccess("203.0.113.10");
            ArgumentCaptor<LoginLogIntegrationEvent> captor =
                    ArgumentCaptor.forClass(LoginLogIntegrationEvent.class);
            verify(publisher).publish(eq(AuthLoginLogPublisher.TOPIC), eq(AuthLoginLogPublisher.TAG), captor.capture());
            LoginLogIntegrationEvent event = captor.getValue();
            assertThat(event.getAccountId()).isEqualTo(1L);
            assertThat(event.getUsername()).isEqualTo("alice");
            assertThat(event.getIp()).isEqualTo("203.0.113.10");
            assertThat(event.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("JWT bearer 认证成功(每个 API 请求)不应记录登录日志")
        void shouldIgnoreJwtBearerAuthenticationSuccess() {
            ClientIpHolder.set("203.0.113.10");
            Jwt jwt = Jwt.withTokenValue("token.value")
                    .header("alg", "RS256")
                    .subject("alice")
                    .build();

            listener.onAuthSuccess(new AuthenticationSuccessEvent(new JwtAuthenticationToken(jwt)));

            verifyNoInteractions(publisher, loginAttemptService);
        }

        @Test
        @DisplayName("OAuth2 客户端认证成功(token 端点/授权码兑换/refresh)不应记录登录日志")
        void shouldIgnoreOAuth2ClientAuthenticationSuccess() {
            ClientIpHolder.set("203.0.113.10");
            RegisteredClient client = RegisteredClient.withId("c-1")
                    .clientId("eagle-web")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .build();
            OAuth2ClientAuthenticationToken clientAuth =
                    new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);

            listener.onAuthSuccess(new AuthenticationSuccessEvent(clientAuth));

            verifyNoInteractions(publisher, loginAttemptService);
        }

        @Test
        @DisplayName("其他类型认证成功不应记录登录日志")
        void shouldIgnoreNonLoginAuthenticationSuccess() {
            ClientIpHolder.set("203.0.113.10");
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("alice", "credentials");

            listener.onAuthSuccess(new AuthenticationSuccessEvent(auth));

            verifyNoInteractions(publisher, loginAttemptService);
        }
    }

    @Nested
    @DisplayName("onAuthFailure")
    class OnAuthFailure {

        @Test
        @DisplayName("应发布登录失败日志事件")
        void shouldPublishLoginFailureEvent() {
            ClientIpHolder.set("203.0.113.20");
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("bob", "bad");
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            listener.onAuthFailure(new AuthenticationFailureBadCredentialsEvent(auth, ex));

            verify(loginAttemptService).registerFailure("203.0.113.20");
            ArgumentCaptor<LoginLogIntegrationEvent> captor =
                    ArgumentCaptor.forClass(LoginLogIntegrationEvent.class);
            verify(publisher).publish(eq(AuthLoginLogPublisher.TOPIC), eq(AuthLoginLogPublisher.TAG), captor.capture());
            LoginLogIntegrationEvent event = captor.getValue();
            assertThat(event.getUsername()).isEqualTo("bob");
            assertThat(event.getIp()).isEqualTo("203.0.113.20");
            assertThat(event.isSuccess()).isFalse();
            assertThat(event.getFailReason()).isEqualTo("Bad credentials");
        }
    }
}

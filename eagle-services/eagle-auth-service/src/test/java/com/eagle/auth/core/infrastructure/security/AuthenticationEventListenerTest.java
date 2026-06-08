package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.infrastructure.event.AuthLoginLogPublisher;
import com.eagle.auth.core.infrastructure.event.LoginLogIntegrationEvent;
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
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
        @DisplayName("应发布登录成功日志事件")
        void shouldPublishLoginSuccessEvent() {
            ClientIpHolder.set("203.0.113.10");
            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken("alice", "credentials");

            listener.onAuthSuccess(new AuthenticationSuccessEvent(auth));

            verify(loginAttemptService).registerSuccess("203.0.113.10");
            ArgumentCaptor<LoginLogIntegrationEvent> captor =
                    ArgumentCaptor.forClass(LoginLogIntegrationEvent.class);
            verify(publisher).publish(eq(AuthLoginLogPublisher.TOPIC), eq(AuthLoginLogPublisher.TAG), captor.capture());
            LoginLogIntegrationEvent event = captor.getValue();
            assertThat(event.getUsername()).isEqualTo("alice");
            assertThat(event.getIp()).isEqualTo("203.0.113.10");
            assertThat(event.isSuccess()).isTrue();
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

package com.eagle.auth.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LoginRateLimitFilter} 单元测试。
 * <p>
 * 使用 fastjson2 序列化响应，测试验证 429 状态码和 JSON Content-Type。
 */
@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private LoginRateLimitFilter filter;

    @Nested
    @DisplayName("doFilterInternal — /login 路径")
    class LoginPath {

        @Test
        @DisplayName("should return 429 and not proceed when IP is blocked")
        void shouldReturn429WhenBlocked() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/login");
            request.setRemoteAddr("1.2.3.4");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            when(loginAttemptService.isBlocked("1.2.3.4")).thenReturn(true);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).contains("application/json");
            // chain 未被调用
            assertThat(chain.getRequest()).isNull();
        }

        @Test
        @DisplayName("should proceed when IP is not blocked")
        void shouldProceedWhenNotBlocked() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/login");
            request.setRemoteAddr("1.2.3.4");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            when(loginAttemptService.isBlocked("1.2.3.4")).thenReturn(false);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("should use X-Forwarded-For header as client IP")
        void shouldUseXForwardedForIp() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/login");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            when(loginAttemptService.isBlocked("203.0.113.5")).thenReturn(false);

            filter.doFilter(request, response, chain);

            verify(loginAttemptService).isBlocked("203.0.113.5");
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("should skip filtering for non-login paths")
        void shouldSkipNonLoginPath() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/api/users");

            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("should not skip filtering for login path")
        void shouldApplyToLoginPath() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/login");

            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }
}

package com.eagle.system.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RequestIpResolver")
class RequestIpResolverTest {

    private RequestIpResolver resolver;

    @BeforeEach
    void setUp() {
        TrustedProxyProperties props = new TrustedProxyProperties();
        props.setTrustedProxies(List.of("127.0.0.0/8", "10.0.0.0/8"));
        resolver = new RequestIpResolver(props);
        resolver.init();
    }

    @Test
    @DisplayName("returns remoteAddr when client connects directly (not via trusted proxy)")
    void directConnectIgnoresXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.5");
        req.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        assertEquals("203.0.113.5", resolver.resolve(req));
    }

    @Test
    @DisplayName("returns rightmost-non-proxy hop when remoteAddr is trusted proxy")
    void trustedProxyUsesXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.99");
        // 反向遍历：10.0.0.99 是可信代理 → 跳过；203.0.113.5 是真实客户端
        assertEquals("203.0.113.5", resolver.resolve(req));
    }

    @Test
    @DisplayName("returns remoteAddr when XFF header is absent")
    void noXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.5");
        assertEquals("10.0.0.5", resolver.resolve(req));
    }

    @Test
    @DisplayName("attacker-injected XFF from untrusted IP is ignored")
    void attackerInjectedXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("198.51.100.1");
        // 攻击者尝试构造一个看起来内网的 IP
        req.addHeader("X-Forwarded-For", "10.0.0.99");
        assertEquals("198.51.100.1", resolver.resolve(req));
    }

    @Test
    @DisplayName("ipv6 loopback is treated as trusted when configured")
    void ipv6Loopback() {
        TrustedProxyProperties props = new TrustedProxyProperties();
        props.setTrustedProxies(List.of("::1/128"));
        RequestIpResolver r = new RequestIpResolver(props);
        r.init();

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("::1");
        req.addHeader("X-Forwarded-For", "203.0.113.5");
        assertEquals("203.0.113.5", r.resolve(req));
    }
}

package com.eagle.auth.core.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppleAppAuthenticationConverterTest {

    @Test
    void convertsAppleCredential() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "apple_app");
        request.setParameter("identity_token", "signed-jwt");
        request.setParameter("nonce", "nonce-1");
        request.setParameter("full_name", "小明");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("eagleApp", null));

        try {
            AppleAppAuthenticationToken token = assertInstanceOf(
                    AppleAppAuthenticationToken.class,
                    new AppleAppAuthenticationConverter().convert(request));
            assertEquals("signed-jwt", token.getIdentityToken());
            assertEquals("nonce-1", token.getNonce());
            assertEquals("小明", token.getFullName());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void ignoresOtherGrant() {
        assertNull(new AppleAppAuthenticationConverter().convert(new MockHttpServletRequest()));
    }

    @Test
    void trimsAndLimitsUntrustedFullName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "apple_app");
        request.setParameter("identity_token", "signed-jwt");
        request.setParameter("nonce", "nonce-1");
        request.setParameter("full_name", "  " + "名".repeat(80) + "  ");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("eagleApp", null));

        try {
            AppleAppAuthenticationToken token = assertInstanceOf(
                    AppleAppAuthenticationToken.class,
                    new AppleAppAuthenticationConverter().convert(request));
            assertEquals(64, token.getFullName().length());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

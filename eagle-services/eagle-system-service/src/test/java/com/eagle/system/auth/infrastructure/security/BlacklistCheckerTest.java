package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.exception.AppException;
import com.eagle.system.auth.application.service.BlacklistApplicationService;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistCheckerTest {

    @Mock
    BlacklistApplicationService blacklist;
    @InjectMocks
    BlacklistChecker checker;

    @Test
    void shouldThrowWhenIpBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.IP, "1.1.1.1")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, null, "1.1.1.1", null));
        assertEquals(AuthErrorCode.IP_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldThrowWhenPhoneBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.PHONE, "13800138000")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, "13800138000", null, null));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldThrowWhenAccountIdBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, "123")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, null, null, 123L));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldPassWhenNotBlacklisted() {
        assertDoesNotThrow(() -> checker.checkLogin("alice", "13800138000", "1.1.1.1", 1L));
    }

    @Test
    void checkAccountShouldThrowWhenAccountIdBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, "456")).thenReturn(true);
        AppException ex = assertThrows(AppException.class, () -> checker.checkAccount(456L));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void checkAccountShouldNoopWhenNullId() {
        assertDoesNotThrow(() -> checker.checkAccount(null));
    }
}

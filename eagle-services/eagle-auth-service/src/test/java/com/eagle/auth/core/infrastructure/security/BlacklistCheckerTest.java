package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.BlacklistApplicationService;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.enums.BlacklistType;
import com.eagle.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

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
    @DisplayName("IP 命中黑名单时应抛出异常")
    void shouldThrowWhenIpBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.IP, "1.1.1.1")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, null, "1.1.1.1", null));
        assertEquals(AuthErrorCode.IP_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("手机号命中黑名单时应抛出异常")
    void shouldThrowWhenPhoneBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.PHONE, "13800138000")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, "13800138000", null, null));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("账号 ID 命中黑名单时应抛出异常")
    void shouldThrowWhenAccountIdBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, "123")).thenReturn(true);
        AppException ex = assertThrows(AppException.class,
                () -> checker.checkLogin(null, null, null, 123L));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("未命中黑名单时应正常通过")
    void shouldPassWhenNotBlacklisted() {
        assertDoesNotThrow(() -> checker.checkLogin("alice", "13800138000", "1.1.1.1", 1L));
    }

    @Test
    @DisplayName("检查账号时账号 ID 命中黑名单应抛出异常")
    void checkAccountShouldThrowWhenAccountIdBlacklisted() {
        when(blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, "456")).thenReturn(true);
        AppException ex = assertThrows(AppException.class, () -> checker.checkAccount(456L));
        assertEquals(AuthErrorCode.IDENTITY_BLACKLISTED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("账号 ID 为空时检查账号应为空操作")
    void checkAccountShouldNoopWhenNullId() {
        assertDoesNotThrow(() -> checker.checkAccount(null));
    }
}

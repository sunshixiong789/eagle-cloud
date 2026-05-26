package com.eagle.auth.core.infrastructure.event;

import com.eagle.auth.core.domain.event.BlacklistAddedEvent;
import com.eagle.auth.core.domain.event.BlacklistRemovedEvent;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.BlacklistType;
import com.eagle.auth.core.domain.port.OnlineUserPort;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.infrastructure.cache.BlacklistCacheStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistCacheSyncHandlerTest {

    @Mock
    BlacklistCacheStore cacheStore;
    @Mock
    OnlineUserPort onlineUserPort;
    @Mock
    AccountRepository accountRepository;
    @InjectMocks
    BlacklistCacheSyncHandler handler;

    @Nested
    @DisplayName("onAdded")
    class OnAdded {

        @Test
        void shouldForceLogoutAllSessionsForAccountId() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.ACCOUNT_ID, "42", null);
            when(onlineUserPort.listJtisByAccount(42L)).thenReturn(List.of("jti-a", "jti-b"));

            handler.onAdded(event);

            verify(cacheStore).add(BlacklistType.ACCOUNT_ID, "42");
            verify(onlineUserPort).forceLogout("jti-a");
            verify(onlineUserPort).forceLogout("jti-b");
        }

        @Test
        void shouldResolveAccountByPhoneAndForceLogout() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.PHONE, "13800138000", null);
            Account account = mockAccount(99L);
            when(accountRepository.findByPhone("13800138000")).thenReturn(Optional.of(account));
            when(onlineUserPort.listJtisByAccount(99L)).thenReturn(List.of("jti-x"));

            handler.onAdded(event);

            verify(cacheStore).add(BlacklistType.PHONE, "13800138000");
            verify(onlineUserPort).forceLogout("jti-x");
        }

        @Test
        void shouldFallbackToUnionidWhenOpenidNotFound() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.OPENID, "ox", null);
            Account account = mockAccount(7L);
            when(accountRepository.findByWechatBindingOpenid("ox")).thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingUnionid("ox")).thenReturn(Optional.of(account));
            when(onlineUserPort.listJtisByAccount(7L)).thenReturn(List.of("jti-y"));

            handler.onAdded(event);

            verify(onlineUserPort).forceLogout("jti-y");
        }

        @Test
        void shouldSkipLogoutWhenIpType() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.IP, "1.1.1.1", null);

            handler.onAdded(event);

            verify(cacheStore).add(BlacklistType.IP, "1.1.1.1");
            verify(onlineUserPort, never()).forceLogout(any());
            verifyNoInteractions(accountRepository);
        }

        @Test
        void shouldSkipLogoutWhenEmailType() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.EMAIL, "x@y.com", null);

            handler.onAdded(event);

            verify(cacheStore).add(BlacklistType.EMAIL, "x@y.com");
            verify(onlineUserPort, never()).forceLogout(any());
            verifyNoInteractions(accountRepository);
        }

        @Test
        void shouldSkipLogoutWhenAccountIdValueNotNumeric() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.ACCOUNT_ID, "not-a-number", null);

            handler.onAdded(event);

            verify(cacheStore).add(BlacklistType.ACCOUNT_ID, "not-a-number");
            verify(onlineUserPort, never()).forceLogout(any());
        }

        @Test
        void shouldSkipLogoutWhenPhoneNotMappedToAccount() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.PHONE, "13900139000", null);
            when(accountRepository.findByPhone("13900139000")).thenReturn(Optional.empty());

            handler.onAdded(event);

            verify(onlineUserPort, never()).forceLogout(any());
        }

        @Test
        void shouldSkipLogoutWhenAccountHasNoActiveSessions() {
            BlacklistAddedEvent event = new BlacklistAddedEvent(1L, BlacklistType.ACCOUNT_ID, "42", null);
            when(onlineUserPort.listJtisByAccount(42L)).thenReturn(List.of());

            handler.onAdded(event);

            verify(onlineUserPort).listJtisByAccount(42L);
            verify(onlineUserPort, never()).forceLogout(any());
        }
    }

    @Nested
    @DisplayName("onRemoved")
    class OnRemoved {

        @Test
        void shouldEvictCacheOnly() {
            BlacklistRemovedEvent event = new BlacklistRemovedEvent(1L, BlacklistType.ACCOUNT_ID, "42");

            handler.onRemoved(event);

            verify(cacheStore).remove(BlacklistType.ACCOUNT_ID, "42");
            verifyNoInteractions(onlineUserPort);
            verifyNoInteractions(accountRepository);
        }
    }

    private Account mockAccount(Long id) {
        Account account = org.mockito.Mockito.mock(Account.class);
        when(account.getId()).thenReturn(id);
        return account;
    }
}

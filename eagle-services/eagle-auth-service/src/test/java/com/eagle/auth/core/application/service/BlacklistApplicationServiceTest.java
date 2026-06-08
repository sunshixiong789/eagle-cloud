package com.eagle.auth.core.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.auth.core.application.command.AddBlacklistCommand;
import com.eagle.auth.core.application.mapper.BlacklistMapper;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Blacklist;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.BlacklistType;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.repository.BlacklistRepository;
import com.eagle.auth.core.infrastructure.cache.BlacklistCacheStore;
import com.eagle.auth.core.infrastructure.config.AdminProperties;
import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistApplicationServiceTest {

    @Mock
    BlacklistRepository repository;
    @Mock
    BlacklistMapper mapper;
    @Mock
    BlacklistCacheStore cacheStore;
    @Mock
    AccountRepository accountRepository;
    @Mock
    AdminProperties adminProperties;
    @InjectMocks
    BlacklistApplicationService service;

    @Nested
    @DisplayName("addToBlacklist")
    class Add {
        @Test
        @DisplayName("应保存")
        void shouldSave() {
            when(repository.findByTypeAndValue(BlacklistType.PHONE, "13800138000"))
                    .thenReturn(Optional.empty());
            when(repository.save(any(Blacklist.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.addToBlacklist(new AddBlacklistCommand(
                    BlacklistType.PHONE, "13800138000", "test", null, 99L, "admin"));

            verify(repository).save(any(Blacklist.class));
        }

        @Test
        @DisplayName("应拒绝重复")
        void shouldRejectDuplicate() {
            when(repository.findByTypeAndValue(BlacklistType.PHONE, "13800138000"))
                    .thenReturn(Optional.of(Blacklist.create(
                            BlacklistType.PHONE, "13800138000", null, null, null, null)));

            assertThrows(ConflictException.class,
                    () -> service.addToBlacklist(new AddBlacklistCommand(
                            BlacklistType.PHONE, "13800138000", null, null, 99L, "admin")));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("应拒绝管理员账号ID")
        void shouldRejectAdminAccountId() {
            Account admin = Account.create("admin", "{bcrypt}encoded", "13800138000", null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(100L)).thenReturn(Optional.of(admin));

            AppException ex = assertThrows(DomainException.class,
                    () -> service.addToBlacklist(new AddBlacklistCommand(
                            BlacklistType.ACCOUNT_ID, "100",
                            null, null, 99L, "operator")));

            assertEquals(AuthErrorCode.ADMIN_ACCOUNT_PROTECTED, ex.getErrorCode());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeFromBlacklist")
    class Remove {
        @Test
        @DisplayName("不Found")
        void notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.removeFromBlacklist(99L));
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlack {

        @Test
        @DisplayName("缓存未命中")
        void cacheMiss() {
            when(cacheStore.isMember(BlacklistType.IP, "1.1.1.1")).thenReturn(false);
            assertFalse(service.isBlacklisted(BlacklistType.IP, "1.1.1.1"));
            verify(repository, never()).findByTypeAndValue(any(), any());
        }

        @Test
        @DisplayName("命中")
        void hit() {
            when(cacheStore.isMember(BlacklistType.IP, "1.1.1.1")).thenReturn(true);
            when(repository.findByTypeAndValue(BlacklistType.IP, "1.1.1.1"))
                    .thenReturn(Optional.of(Blacklist.create(
                            BlacklistType.IP, "1.1.1.1", null, null, null, null)));
            assertTrue(service.isBlacklisted(BlacklistType.IP, "1.1.1.1"));
        }

        @Test
        @DisplayName("Db缺失时应清理缓存")
        void shouldEvictCacheWhenDbMissing() {
            when(cacheStore.isMember(BlacklistType.IP, "1.1.1.1")).thenReturn(true);
            when(repository.findByTypeAndValue(BlacklistType.IP, "1.1.1.1"))
                    .thenReturn(Optional.empty());

            assertFalse(service.isBlacklisted(BlacklistType.IP, "1.1.1.1"));

            verify(cacheStore).remove(BlacklistType.IP, "1.1.1.1");
            // 关键：不再走 removeFromBlacklist（同类调用会失效事务），
            // 也就不应触发 repository.delete / publishRemovedEvent。
            verify(repository, never()).delete(any());
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Expired时应清理缓存")
        void shouldEvictCacheWhenExpired() {
            Blacklist expired = Blacklist.create(
                    BlacklistType.IP, "1.1.1.1", null,
                    LocalDateTime.now().plusDays(1), 99L, "admin");
            // 直接构造一个已过期的实例（构造时强制 expiresAt 未来，再用反射不可行，
            // 改为 mock isExpired 行为）
            Blacklist expiredSpy = org.mockito.Mockito.spy(expired);
            org.mockito.Mockito.doReturn(true).when(expiredSpy).isExpired(any());

            when(cacheStore.isMember(BlacklistType.IP, "1.1.1.1")).thenReturn(true);
            when(repository.findByTypeAndValue(BlacklistType.IP, "1.1.1.1"))
                    .thenReturn(Optional.of(expiredSpy));

            assertFalse(service.isBlacklisted(BlacklistType.IP, "1.1.1.1"));

            verify(cacheStore).remove(BlacklistType.IP, "1.1.1.1");
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("purgeExpired")
    class Purge {
        @Test
        @DisplayName("lyExpired时应Purge")
        void shouldPurgeOnlyExpired() {
            Blacklist active = Blacklist.create(
                    BlacklistType.IP, "1.1.1.1", null, null, 1L, "admin");
            Blacklist expired = org.mockito.Mockito.spy(Blacklist.create(
                    BlacklistType.IP, "2.2.2.2", null,
                    LocalDateTime.now().plusDays(1), 1L, "admin"));
            org.mockito.Mockito.doReturn(true).when(expired).isExpired(any());

            org.springframework.data.domain.Page<Blacklist> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(active, expired));
            when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);

            int n = service.purgeExpired();
            assertEquals(1, n);
            verify(repository).delete(expired);
            verify(repository, never()).delete(active);
        }
    }
}

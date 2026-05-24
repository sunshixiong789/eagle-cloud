package com.eagle.system.base.application.service;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountEventApplicationService")
class AccountEventApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private AccountEventApplicationService service;

    @Nested
    @DisplayName("onAccountRegistered")
    class OnRegistered {

        @Test
        @DisplayName("首次事件应创建 User")
        void createsUserOnFirstSeen() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");
            event.setPhone("13900000000");

            when(userRepository.existsByAccountId(100L)).thenReturn(false);
            Role role = Mockito.mock(Role.class);
            when(role.getId()).thenReturn(7L);
            when(roleRepository.findByRoleCode("user")).thenReturn(Optional.of(role));

            service.onAccountRegistered(event);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("existsByAccountId 已存在(显式重复) 跳过 save")
        void skipsWhenBusinessDuplicate() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");
            when(userRepository.existsByAccountId(100L)).thenReturn(true);

            service.onAccountRegistered(event);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("并发窗口下 DB unique 约束兜住 — 捕获 DataIntegrityViolation 静默跳过")
        void swallowsDataIntegrityViolationOnConcurrentInsert() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");

            when(userRepository.existsByAccountId(100L)).thenReturn(false);
            Role role = Mockito.mock(Role.class);
            when(role.getId()).thenReturn(7L);
            when(roleRepository.findByRoleCode("user")).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_account_id violation"));

            // 不应抛异常 — RocketMQ consumer 不会重试,不会进 DLQ(真实重复)
            service.onAccountRegistered(event);
        }
    }

    @Nested
    @DisplayName("onAccountDeleted")
    class OnDeleted {

        @Test
        @DisplayName("找到 User 时删除并清理缓存")
        void deletesAndEvicts() {
            AccountDeletedMessage event = new AccountDeletedMessage();
            event.setAccountId(200L);
            User user = User.createForAccount(200L, "bob", "13800000000", null);
            when(userRepository.findByAccountId(200L)).thenReturn(Optional.of(user));

            service.onAccountDeleted(event);

            verify(userRepository).delete(user);
            verify(cacheManager).getCache("USER_NAME");
        }

        @Test
        @DisplayName("找不到 User 时静默跳过(已级联删除)")
        void skipsWhenUserAbsent() {
            AccountDeletedMessage event = new AccountDeletedMessage();
            event.setAccountId(200L);
            when(userRepository.findByAccountId(200L)).thenReturn(Optional.empty());

            service.onAccountDeleted(event);

            verify(userRepository, never()).delete(any(User.class));
        }
    }
}

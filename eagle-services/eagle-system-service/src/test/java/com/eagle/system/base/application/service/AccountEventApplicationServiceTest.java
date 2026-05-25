package com.eagle.system.base.application.service;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.config.AdminProperties;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private AdminProperties adminProperties;
    @InjectMocks
    private AccountEventApplicationService service;

    @Nested
    @DisplayName("onAccountRegistered")
    class OnRegistered {

        @Test
        @DisplayName("首次事件应创建 User 并仅分配 user 角色")
        void createsUserOnFirstSeen() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");
            event.setPhone("13900000000");

            when(userRepository.existsByAccountId(100L)).thenReturn(false);
            Role userRole = Mockito.mock(Role.class);
            when(userRole.getId()).thenReturn(7L);
            when(roleRepository.findByRoleCode("user")).thenReturn(Optional.of(userRole));
            when(adminProperties.getUsername()).thenReturn("admin");

            service.onAccountRegistered(event);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRoleIds()).containsExactly(7L);
            verify(roleRepository, never()).findByRoleCode("admin");
        }

        @Test
        @DisplayName("username 匹配 eagle.admin.username 时分配 user + admin 双角色")
        void assignsAdminRoleWhenUsernameMatches() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(1L);
            event.setUsername("admin");

            when(userRepository.existsByAccountId(1L)).thenReturn(false);
            Role userRole = Mockito.mock(Role.class);
            when(userRole.getId()).thenReturn(7L);
            Role adminRole = Mockito.mock(Role.class);
            when(adminRole.getId()).thenReturn(1L);
            when(roleRepository.findByRoleCode("user")).thenReturn(Optional.of(userRole));
            when(roleRepository.findByRoleCode("admin")).thenReturn(Optional.of(adminRole));
            when(adminProperties.getUsername()).thenReturn("admin");

            service.onAccountRegistered(event);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRoleIds()).containsExactlyInAnyOrder(7L, 1L);
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
            when(adminProperties.getUsername()).thenReturn("admin");
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

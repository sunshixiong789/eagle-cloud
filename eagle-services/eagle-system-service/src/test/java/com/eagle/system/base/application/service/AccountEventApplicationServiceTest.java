package com.eagle.system.base.application.service;

import com.eagle.rocketmq.idempotency.IdempotencyChecker;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.messaging.event.AccountDeletedMessage;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
    private IdempotencyChecker idempotencyChecker;
    @InjectMocks
    private AccountEventApplicationService service;

    @Nested
    @DisplayName("onAccountRegistered")
    class OnRegistered {

        @BeforeEach
        void setUp() {
            // 默认: 不查重复(让首次链路跑完)
            lenient().when(idempotencyChecker.isDuplicate(anyString())).thenReturn(false);
        }

        @Test
        @DisplayName("首次事件应创建 User")
        void createsUserOnFirstSeen() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");
            event.setPhone("13900000000");

            when(userRepository.existsByAccountId(100L)).thenReturn(false);
            Role role = org.mockito.Mockito.mock(Role.class);
            when(role.getId()).thenReturn(7L);
            when(roleRepository.findByRoleCode("user")).thenReturn(Optional.of(role));

            service.onAccountRegistered(event);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("idempotency duplicate 直接跳过, 不查 UserRepository")
        void skipsWhenDuplicate() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");
            when(idempotencyChecker.isDuplicate(anyString())).thenReturn(true);

            service.onAccountRegistered(event);

            verify(userRepository, never()).existsByAccountId(anyLong());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("通过幂等但 existsByAccountId 已存在(幂等键过期场景) 也跳过 save")
        void skipsWhenBusinessDuplicate() {
            AccountRegisteredMessage event = new AccountRegisteredMessage();
            event.setAccountId(100L);
            event.setUsername("alice");
            when(userRepository.existsByAccountId(100L)).thenReturn(true);

            service.onAccountRegistered(event);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("onAccountDeleted")
    class OnDeleted {

        @BeforeEach
        void setUp() {
            lenient().when(idempotencyChecker.isDuplicate(anyString())).thenReturn(false);
        }

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
        @DisplayName("idempotency duplicate 直接跳过, 不查 UserRepository")
        void skipsWhenDuplicate() {
            AccountDeletedMessage event = new AccountDeletedMessage();
            event.setAccountId(200L);
            when(idempotencyChecker.isDuplicate(anyString())).thenReturn(true);

            service.onAccountDeleted(event);

            verify(userRepository, never()).findByAccountId(anyLong());
            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("通过幂等但找不到 User 时静默跳过(级联删除已生效)")
        void skipsWhenUserAbsent() {
            AccountDeletedMessage event = new AccountDeletedMessage();
            event.setAccountId(200L);
            when(userRepository.findByAccountId(200L)).thenReturn(Optional.empty());

            service.onAccountDeleted(event);

            verify(userRepository, never()).delete(any(User.class));
        }
    }
}

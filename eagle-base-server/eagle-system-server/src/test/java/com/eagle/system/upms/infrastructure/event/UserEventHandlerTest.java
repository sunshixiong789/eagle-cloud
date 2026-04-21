package com.eagle.system.infrastructure.event;

import com.eagle.system.domain.model.User;
import com.eagle.system.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserEventHandler 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("用户领域事件处理器")
@ExtendWith(MockitoExtension.class)
class UserEventHandlerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserEventHandler userEventHandler;

    @Nested
    @DisplayName("handleAccountRegistered")
    class HandleAccountRegistered {

        @Test
        @DisplayName("should create user from account registration event")
        void shouldCreateUserFromEvent() {
            // Given
            AccountRegisteredEvent event = new AccountRegisteredEvent(
                1L, "testuser", "13800000000", "昵称", "avatar.jpg",
                "test@example.com", 10L, Set.of(1L, 2L)
            );

            when(userRepository.existsByAccountId(1L)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            userEventHandler.handleAccountRegistered(event);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertEquals(1L, savedUser.getAccountId());
            assertEquals("testuser", savedUser.getUsername());
            assertEquals("test@example.com", savedUser.getEmail());
            assertEquals(10L, savedUser.getDeptId());
            assertEquals(2, savedUser.getRoleIds().size());
            assertNotNull(savedUser.getProfile());
            assertEquals("昵称", savedUser.getProfile().getNickname());
            assertEquals("avatar.jpg", savedUser.getProfile().getAvatar());
        }

        @Test
        @DisplayName("should create user without profile hints")
        void shouldCreateUserWithoutProfileHints() {
            // Given
            AccountRegisteredEvent event = new AccountRegisteredEvent(
                1L, "testuser", "13800000000", null, null,
                null, null, null
            );

            when(userRepository.existsByAccountId(1L)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            userEventHandler.handleAccountRegistered(event);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertNull(savedUser.getProfile());
            assertNull(savedUser.getDeptId());
            assertTrue(savedUser.getRoleIds().isEmpty());
        }

        @Test
        @DisplayName("should skip when user already exists (idempotency)")
        void shouldSkipWhenUserAlreadyExists() {
            // Given
            AccountRegisteredEvent event = new AccountRegisteredEvent(
                1L, "testuser", null, null, null, null, null, null
            );

            when(userRepository.existsByAccountId(1L)).thenReturn(true);

            // When
            userEventHandler.handleAccountRegistered(event);

            // Then
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("handleAccountDeleted")
    class HandleAccountDeleted {

        @Test
        @DisplayName("should delete user and evict cache on account deletion")
        void shouldDeleteUserAndEvictCache() {
            // Given
            AccountDeletedEvent event = new AccountDeletedEvent(1L);
            User user = User.create(1L, "testuser", null, null);
            Cache mockCache = mock(Cache.class);

            when(userRepository.findByAccountId(1L)).thenReturn(Optional.of(user));
            when(cacheManager.getCache("USER_NAME")).thenReturn(mockCache);

            // When
            userEventHandler.handleAccountDeleted(event);

            // Then
            verify(userRepository).delete(user);
            verify(mockCache).evict("testuser");
        }

        @Test
        @DisplayName("should do nothing when user not found for deleted account")
        void shouldDoNothingWhenUserNotFound() {
            // Given
            AccountDeletedEvent event = new AccountDeletedEvent(999L);

            when(userRepository.findByAccountId(999L)).thenReturn(Optional.empty());

            // When
            userEventHandler.handleAccountDeleted(event);

            // Then
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should handle null cache gracefully")
        void shouldHandleNullCacheGracefully() {
            // Given
            AccountDeletedEvent event = new AccountDeletedEvent(1L);
            User user = User.create(1L, "testuser", null, null);

            when(userRepository.findByAccountId(1L)).thenReturn(Optional.of(user));
            when(cacheManager.getCache("USER_NAME")).thenReturn(null);

            // When
            userEventHandler.handleAccountDeleted(event);

            // Then
            verify(userRepository).delete(user);
        }
    }
}

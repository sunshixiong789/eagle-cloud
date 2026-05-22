package com.eagle.system.base.application.service;

import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.Gender;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.interfaces.dto.response.AuthorizationView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthorizationQueryService 为 auth-service 提供 /internal/authorization/{accountId} 数据,
 * 单元测试覆盖:找不到用户、无角色、有角色、profile 缺失四种路径。
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationQueryServiceTest {

    private static final Long ACCOUNT_ID = 100L;

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @InjectMocks
    AuthorizationQueryService service;

    private User sampleUser(Set<Long> roleIds) {
        UserProfile profile = new UserProfile("https://a.png", "Alice", "Alice Real", Gender.FEMALE, "bio");
        User user = User.create(ACCOUNT_ID, "alice", "alice@example.com", profile);
        if (roleIds != null && !roleIds.isEmpty()) {
            user.assignRoles(roleIds);
        }
        return user;
    }

    @Nested
    @DisplayName("findByAccountId")
    class FindByAccountId {

        @Test
        @DisplayName("should return empty when user not found by accountId")
        void shouldReturnEmptyWhenUserMissing() {
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertTrue(service.findByAccountId(ACCOUNT_ID).isEmpty());
            verify(roleRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should return view with empty roleCodes when user has no roles")
        void shouldHandleUserWithoutRoles() {
            when(userRepository.findByAccountId(ACCOUNT_ID))
                    .thenReturn(Optional.of(sampleUser(Set.of())));

            AuthorizationView view = service.findByAccountId(ACCOUNT_ID).orElseThrow();

            assertEquals("Alice Real", view.name());
            assertTrue(view.roleCodes().isEmpty());
            verify(roleRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should map assigned role IDs to role codes")
        void shouldMapRoleCodes() {
            User user = sampleUser(Set.of(1L, 2L));
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(user));
            Role admin = Role.create("Admin", "admin", null, 1);
            Role operator = Role.create("Operator", "operator", null, 2);
            when(roleRepository.findAllById(user.getRoleIds())).thenReturn(List.of(admin, operator));

            AuthorizationView view = service.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertEquals(Set.of("admin", "operator"), view.roleCodes());
        }

        @Test
        @DisplayName("should leave name null when user profile absent")
        void shouldHandleNullProfile() {
            User user = User.create(ACCOUNT_ID, "alice", "alice@example.com", null);
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(user));

            AuthorizationView view = service.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertNull(view.name());
            assertTrue(view.roleCodes().isEmpty());
        }
    }
}

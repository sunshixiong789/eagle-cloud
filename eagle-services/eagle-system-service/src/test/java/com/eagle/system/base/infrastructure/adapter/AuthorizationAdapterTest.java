package com.eagle.system.base.infrastructure.adapter;

import com.eagle.system.auth.domain.port.AuthorizationInfo;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.Gender;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
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
 * AuthorizationAdapter 是 auth 域 {@code AuthorizationPort} 的 system 域 Driven Adapter，
 * 单元测试覆盖：找不到用户、无角色、有角色三种路径。
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationAdapterTest {

    private static final Long ACCOUNT_ID = 100L;

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @InjectMocks AuthorizationAdapter adapter;

    private User sampleUser(Set<Long> roleIds) {
        UserProfile profile = new UserProfile("https://a.png", "Alice", "Alice Real", Gender.FEMALE, "bio");
        User user = User.create(ACCOUNT_ID, "alice", "alice@example.com", profile);
        user.assignDept(42L);
        if (roleIds != null && !roleIds.isEmpty()) {
            user.assignRoles(roleIds);
        }
        return user;
    }

    @Nested
    @DisplayName("findAuthorizationInfo")
    class FindAuthorizationInfo {

        @Test
        @DisplayName("should return empty when user not found by accountId")
        void shouldReturnEmptyWhenUserMissing() {
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertTrue(adapter.findAuthorizationInfo(ACCOUNT_ID).isEmpty());
            verify(roleRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should return info with empty roleCodes when user has no roles")
        void shouldHandleUserWithoutRoles() {
            when(userRepository.findByAccountId(ACCOUNT_ID))
                    .thenReturn(Optional.of(sampleUser(Set.of())));

            AuthorizationInfo info = adapter.findAuthorizationInfo(ACCOUNT_ID).orElseThrow();

            assertEquals("Alice Real", info.name());
            assertEquals(42L, info.deptId());
            assertNull(info.deptName(), "部门管理已下线，deptName 应为 null");
            assertTrue(info.roleCodes().isEmpty());
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

            AuthorizationInfo info = adapter.findAuthorizationInfo(ACCOUNT_ID).orElseThrow();
            assertEquals(Set.of("admin", "operator"), info.roleCodes());
        }

        @Test
        @DisplayName("should leave name null when user profile absent")
        void shouldHandleNullProfile() {
            User user = User.create(ACCOUNT_ID, "alice", "alice@example.com", null);
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(user));

            AuthorizationInfo info = adapter.findAuthorizationInfo(ACCOUNT_ID).orElseThrow();
            assertNull(info.name());
            assertTrue(info.roleCodes().isEmpty());
        }
    }
}

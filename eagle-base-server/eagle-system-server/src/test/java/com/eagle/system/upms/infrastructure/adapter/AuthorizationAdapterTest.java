package com.eagle.system.infrastructure.adapter;

import com.eagle.auth.domain.port.AuthorizationInfo;
import com.eagle.system.domain.model.Dept;
import com.eagle.system.domain.model.Role;
import com.eagle.system.domain.model.User;
import com.eagle.system.domain.model.valueobject.UserProfile;
import com.eagle.system.domain.repository.DeptRepository;
import com.eagle.system.domain.repository.RoleRepository;
import com.eagle.system.domain.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthorizationAdapter 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("授权信息查询适配器")
@ExtendWith(MockitoExtension.class)
class AuthorizationAdapterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeptRepository deptRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthorizationAdapter authorizationAdapter;

    @Nested
    @DisplayName("findAuthorizationInfo")
    class FindAuthorizationInfo {

        @Test
        @DisplayName("should return full authorization info")
        void shouldReturnFullAuthorizationInfo() {
            // Given
            Long accountId = 1L;
            UserProfile profile = new UserProfile(null, "张三", "张三", null, null);
            User user = User.create(accountId, "zhangsan", "test@example.com", profile);
            user.assignDept(10L);
            user.assignRoles(Set.of(1L, 2L));

            Dept dept = Dept.create(null, "技术部", null, null, 1);
            Role role1 = Role.create("管理员", "admin", null, 1);
            Role role2 = Role.create("用户", "user", null, 2);

            when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(user));
            when(deptRepository.findById(10L)).thenReturn(Optional.of(dept));
            when(roleRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(role1, role2));

            // When
            Optional<AuthorizationInfo> result = authorizationAdapter.findAuthorizationInfo(accountId);

            // Then
            assertTrue(result.isPresent());
            AuthorizationInfo info = result.get();
            assertEquals("张三", info.name());
            assertEquals(10L, info.deptId());
            assertEquals("技术部", info.deptName());
            assertEquals(2, info.roleCodes().size());
            assertTrue(info.roleCodes().contains("ROLE_admin"));
            assertTrue(info.roleCodes().contains("ROLE_user"));
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            Long accountId = 999L;
            when(userRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

            // When
            Optional<AuthorizationInfo> result = authorizationAdapter.findAuthorizationInfo(accountId);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return info without dept when deptId is null")
        void shouldReturnInfoWithoutDeptWhenDeptIdNull() {
            // Given
            Long accountId = 1L;
            User user = User.create(accountId, "testuser", null, null);

            when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(user));

            // When
            Optional<AuthorizationInfo> result = authorizationAdapter.findAuthorizationInfo(accountId);

            // Then
            assertTrue(result.isPresent());
            AuthorizationInfo info = result.get();
            assertNull(info.deptName());
            assertNull(info.deptId());
            assertTrue(info.roleCodes().isEmpty());
        }

        @Test
        @DisplayName("should return info with null dept name when dept not found")
        void shouldReturnNullDeptNameWhenDeptNotFound() {
            // Given
            Long accountId = 1L;
            User user = User.create(accountId, "testuser", null, null);
            user.assignDept(99L);

            when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(user));
            when(deptRepository.findById(99L)).thenReturn(Optional.empty());

            // When
            Optional<AuthorizationInfo> result = authorizationAdapter.findAuthorizationInfo(accountId);

            // Then
            assertTrue(result.isPresent());
            assertNull(result.get().deptName());
        }

        @Test
        @DisplayName("should return info without profile name")
        void shouldReturnInfoWithoutProfileName() {
            // Given
            Long accountId = 1L;
            User user = User.create(accountId, "testuser", null, null);

            when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(user));

            // When
            Optional<AuthorizationInfo> result = authorizationAdapter.findAuthorizationInfo(accountId);

            // Then
            assertTrue(result.isPresent());
            assertNull(result.get().name());
        }
    }
}

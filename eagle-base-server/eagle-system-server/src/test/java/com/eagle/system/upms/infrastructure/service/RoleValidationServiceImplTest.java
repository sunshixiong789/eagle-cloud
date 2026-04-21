package com.eagle.system.infrastructure.service;

import com.eagle.common.exception.DomainException;
import com.eagle.system.domain.model.Role;
import com.eagle.system.domain.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RoleValidationServiceImpl 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("角色校验领域服务实现")
@ExtendWith(MockitoExtension.class)
class RoleValidationServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleValidationServiceImpl roleValidationService;

    @Nested
    @DisplayName("validateRoles")
    class ValidateRoles {

        @Test
        @DisplayName("should pass when all roles exist and are active")
        void shouldPassWhenAllRolesExistAndActive() {
            // Given
            Set<Long> roleIds = Set.of(1L, 2L);
            Role role1 = Role.create("管理员", "admin", null, 1);
            Role role2 = Role.create("用户", "user", null, 2);

            when(roleRepository.findAllById(roleIds)).thenReturn(List.of(role1, role2));

            // When & Then (no exception)
            assertDoesNotThrow(() -> roleValidationService.validateRoles(roleIds));
        }

        @Test
        @DisplayName("should throw DomainException when some roles not found")
        void shouldThrowWhenSomeRolesNotFound() {
            // Given
            Set<Long> roleIds = Set.of(1L, 2L, 3L);
            Role role1 = Role.create("管理员", "admin", null, 1);
            Role role2 = Role.create("用户", "user", null, 2);

            when(roleRepository.findAllById(roleIds)).thenReturn(List.of(role1, role2));

            // When & Then
            assertThrows(DomainException.class, () ->
                roleValidationService.validateRoles(roleIds));
        }

        @Test
        @DisplayName("should throw DomainException when some roles are disabled")
        void shouldThrowWhenSomeRolesDisabled() {
            // Given
            Set<Long> roleIds = Set.of(1L, 2L);
            Role role1 = Role.create("管理员", "admin", null, 1);
            Role role2 = Role.create("用户", "user", null, 2);
            role2.disable();

            when(roleRepository.findAllById(roleIds)).thenReturn(List.of(role1, role2));

            // When & Then
            assertThrows(DomainException.class, () ->
                roleValidationService.validateRoles(roleIds));
        }

        @Test
        @DisplayName("should pass when roleIds is null")
        void shouldPassWhenRoleIdsNull() {
            // When & Then (no exception)
            assertDoesNotThrow(() -> roleValidationService.validateRoles(null));
            verify(roleRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("should pass when roleIds is empty")
        void shouldPassWhenRoleIdsEmpty() {
            // When & Then (no exception)
            assertDoesNotThrow(() -> roleValidationService.validateRoles(Set.of()));
            verify(roleRepository, never()).findAllById(any());
        }
    }
}

package com.eagle.system.application.service;

import com.eagle.common.exception.NotFoundException;
import com.eagle.system.application.mapper.RoleMapper;
import com.eagle.system.application.mapper.UserMapper;
import com.eagle.system.domain.model.Role;
import com.eagle.system.domain.model.User;
import com.eagle.system.domain.repository.RoleRepository;
import com.eagle.system.domain.repository.UserRepository;
import com.eagle.system.web.dto.request.CreateRoleRequest;
import com.eagle.system.web.dto.request.RoleQueryRequest;
import com.eagle.system.web.dto.request.UpdateRoleRequest;
import com.eagle.system.web.dto.response.RoleResponse;
import com.eagle.system.web.dto.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RoleApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("角色应用服务")
@ExtendWith(MockitoExtension.class)
class RoleApplicationServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RoleApplicationService roleApplicationService;

    @Nested
    @DisplayName("createRole")
    class CreateRole {

        @Test
        @DisplayName("should create role successfully")
        void shouldCreateRoleSuccessfully() {
            // Given
            CreateRoleRequest request = new CreateRoleRequest();
            request.setRoleName("管理员");
            request.setRoleCode("admin");
            request.setRoleDesc("系统管理员");
            request.setSortOrder(1);

            RoleResponse expectedResponse = new RoleResponse();

            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roleMapper.toResponse(any(Role.class))).thenReturn(expectedResponse);

            // When
            RoleResponse result = roleApplicationService.createRole(request);

            // Then
            assertNotNull(result);
            verify(roleRepository).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("updateRole")
    class UpdateRole {

        @Test
        @DisplayName("should update role successfully")
        void shouldUpdateRoleSuccessfully() {
            // Given
            Long id = 1L;
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setRoleName("超级管理员");
            request.setRoleDesc("新描述");
            request.setSortOrder(2);

            Role existingRole = Role.create("管理员", "admin", "旧描述", 1);
            RoleResponse expectedResponse = new RoleResponse();

            when(roleRepository.findById(id)).thenReturn(Optional.of(existingRole));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roleMapper.toResponse(any(Role.class))).thenReturn(expectedResponse);

            // When
            RoleResponse result = roleApplicationService.updateRole(id, request);

            // Then
            assertNotNull(result);
            assertEquals("超级管理员", existingRole.getRoleName());
            assertEquals("新描述", existingRole.getRoleDesc());
            assertEquals(2, existingRole.getSortOrder());
            verify(roleRepository).save(existingRole);
        }

        @Test
        @DisplayName("should throw NotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            // Given
            Long id = 999L;
            UpdateRoleRequest request = new UpdateRoleRequest();

            when(roleRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                roleApplicationService.updateRole(id, request));
        }
    }

    @Nested
    @DisplayName("deleteRole")
    class DeleteRole {

        @Test
        @DisplayName("should delete role successfully")
        void shouldDeleteRoleSuccessfully() {
            // Given
            Long id = 1L;

            // When
            roleApplicationService.deleteRole(id);

            // Then
            verify(roleRepository).deleteById(id);
        }
    }

    @Nested
    @DisplayName("getRoleById")
    class GetRoleById {

        @Test
        @DisplayName("should return role response when found")
        void shouldReturnRoleResponse() {
            // Given
            Long id = 1L;
            Role role = Role.create("管理员", "admin", "描述", 1);
            RoleResponse expectedResponse = new RoleResponse();

            when(roleRepository.findById(id)).thenReturn(Optional.of(role));
            when(roleMapper.toResponse(role)).thenReturn(expectedResponse);

            // When
            RoleResponse result = roleApplicationService.getRoleById(id);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw NotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            // Given
            Long id = 999L;
            when(roleRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                roleApplicationService.getRoleById(id));
        }
    }

    @Nested
    @DisplayName("listRoles")
    class ListRoles {

        @Test
        @DisplayName("should return paginated roles")
        void shouldReturnPaginatedRoles() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            Role role = Role.create("管理员", "admin", "描述", 1);
            Page<Role> rolePage = new PageImpl<>(List.of(role));
            RoleResponse response = new RoleResponse();

            when(roleRepository.findAll(pageable)).thenReturn(rolePage);
            when(roleMapper.toResponse(role)).thenReturn(response);

            // When
            Page<RoleResponse> result = roleApplicationService.listRoles(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("queryRoles")
    class QueryRoles {

        @Test
        @DisplayName("should return roles matching query")
        @SuppressWarnings("unchecked")
        void shouldReturnRolesMatchingQuery() {
            // Given
            RoleQueryRequest request = new RoleQueryRequest();
            request.setRoleName("管理员");
            Pageable pageable = Pageable.ofSize(10);

            Role role = Role.create("管理员", "admin", "描述", 1);
            Page<Role> rolePage = new PageImpl<>(List.of(role));
            RoleResponse response = new RoleResponse();

            when(roleRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(rolePage);
            when(roleMapper.toResponse(role)).thenReturn(response);

            // When
            Page<RoleResponse> result = roleApplicationService.queryRoles(request, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("getUsersByRoleId")
    class GetUsersByRoleId {

        @Test
        @DisplayName("should return users with specified role")
        void shouldReturnUsersWithRole() {
            // Given
            Long roleId = 1L;
            Pageable pageable = Pageable.ofSize(10);
            Role role = Role.create("管理员", "admin", "描述", 1);
            User user = User.create(1L, "testuser", null, null);
            Page<User> userPage = new PageImpl<>(List.of(user));
            UserResponse userResponse = new UserResponse();

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.findByRoleId(roleId, pageable)).thenReturn(userPage);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            // When
            Page<UserResponse> result = roleApplicationService.getUsersByRoleId(roleId, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("should throw NotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            // Given
            Long roleId = 999L;
            Pageable pageable = Pageable.ofSize(10);

            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                roleApplicationService.getUsersByRoleId(roleId, pageable));
        }
    }

    @Nested
    @DisplayName("enableRole")
    class EnableRole {

        @Test
        @DisplayName("should enable role successfully")
        void shouldEnableRoleSuccessfully() {
            // Given
            Long id = 1L;
            Role role = Role.create("管理员", "admin", "描述", 1);
            role.disable();

            when(roleRepository.findById(id)).thenReturn(Optional.of(role));

            // When
            roleApplicationService.enableRole(id);

            // Then
            assertTrue(role.isActive());
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("should throw NotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            // Given
            Long id = 999L;
            when(roleRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                roleApplicationService.enableRole(id));
        }
    }

    @Nested
    @DisplayName("disableRole")
    class DisableRole {

        @Test
        @DisplayName("should disable role successfully")
        void shouldDisableRoleSuccessfully() {
            // Given
            Long id = 1L;
            Role role = Role.create("管理员", "admin", "描述", 1);

            when(roleRepository.findById(id)).thenReturn(Optional.of(role));

            // When
            roleApplicationService.disableRole(id);

            // Then
            assertFalse(role.isActive());
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("should throw NotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            // Given
            Long id = 999L;
            when(roleRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                roleApplicationService.disableRole(id));
        }
    }
}

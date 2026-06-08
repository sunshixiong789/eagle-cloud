package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.base.application.mapper.RoleMapper;
import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.enums.DataScope;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.interfaces.dto.request.CreateRoleRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateRoleRequest;
import com.eagle.system.base.interfaces.dto.response.RoleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RoleApplicationServiceTest {

    private static final Long ID = 10L;

    @Mock
    RoleRepository roleRepository;
    @Mock
    RoleMapper roleMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @InjectMocks
    RoleApplicationService service;

    private Role businessRole() {
        return Role.create("Manager", "manager", "manager role", 10);
    }

    private Role systemRole() {
        return Role.createSystemRole("Super", "super_admin", "system", 1, DataScope.ALL);
    }

    @Nested
    @DisplayName("createRole")
    class Create {
        @Test
        @DisplayName("应创建")
        void shouldCreate() {
            CreateRoleRequest req = new CreateRoleRequest();
            req.setRoleName("Manager");
            req.setRoleCode("manager");
            req.setRoleDesc("desc");
            req.setSortOrder(10);
            Role saved = businessRole();
            when(roleRepository.save(any(Role.class))).thenReturn(saved);
            when(roleMapper.toResponse(saved)).thenReturn(new RoleResponse());

            service.createRole(req);

            verify(roleRepository).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("updateRole")
    class Update {
        @Test
        @DisplayName("应更新")
        void shouldUpdate() {
            Role role = businessRole();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            when(roleRepository.save(role)).thenReturn(role);
            when(roleMapper.toResponse(role)).thenReturn(new RoleResponse());

            UpdateRoleRequest req = new UpdateRoleRequest();
            req.setRoleName("New Name");
            service.updateRole(ID, req);
            assertEquals("New Name", role.getRoleName());
        }

        @Test
        @DisplayName("应拒绝System角色")
        void shouldRejectSystemRole() {
            Role role = systemRole();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            AppException ex = assertThrows(DomainException.class,
                    () -> service.updateRole(ID, new UpdateRoleRequest()));
            assertEquals(SystemErrorCode.ROLE_SYSTEM_PROTECTED, ex.getErrorCode());
            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("缺失时应抛出")
        void shouldThrowWhenMissing() {
            when(roleRepository.findById(ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.updateRole(ID, new UpdateRoleRequest()));
            assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("deleteRole")
    class Delete {
        @Test
        @DisplayName("应删除")
        void shouldDelete() {
            Role role = businessRole();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            service.deleteRole(ID);
            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("应拒绝System角色")
        void shouldRejectSystemRole() {
            Role role = systemRole();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            AppException ex = assertThrows(DomainException.class, () -> service.deleteRole(ID));
            assertEquals(SystemErrorCode.ROLE_SYSTEM_PROTECTED, ex.getErrorCode());
            verify(roleRepository, never()).delete(any(Role.class));
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class EnableDisable {
        @Test
        @DisplayName("应启用")
        void shouldEnable() {
            Role role = businessRole();
            role.disable();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            service.enableRole(ID);
            assertTrue(role.isActive());
        }

        @Test
        @DisplayName("应禁用")
        void shouldDisable() {
            Role role = businessRole();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            service.disableRole(ID);
            assertFalse(role.isActive());
        }

        @Test
        @DisplayName("应拒绝System角色禁用")
        void shouldRejectSystemRoleDisable() {
            Role role = systemRole();
            when(roleRepository.findById(ID)).thenReturn(Optional.of(role));
            AppException ex = assertThrows(DomainException.class, () -> service.disableRole(ID));
            assertEquals(SystemErrorCode.ROLE_SYSTEM_PROTECTED, ex.getErrorCode());
        }
    }
}

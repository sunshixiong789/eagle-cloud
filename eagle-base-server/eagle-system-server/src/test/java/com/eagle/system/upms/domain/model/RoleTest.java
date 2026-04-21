package com.eagle.system.domain.model;

import com.eagle.system.domain.model.enums.DataScope;
import com.eagle.system.domain.model.enums.RoleStatus;
import com.eagle.system.domain.model.enums.RoleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Role 聚合根单元测试
 *
 * @author sunshixiong
 */
@DisplayName("Role 聚合根")
class RoleTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create business role with default values")
        void shouldCreateBusinessRoleWithDefaults() {
            // When
            Role role = Role.create("管理员", "admin", "系统管理员", 1);

            // Then
            assertNotNull(role);
            assertEquals("管理员", role.getRoleName());
            assertEquals("admin", role.getRoleCode());
            assertEquals("系统管理员", role.getRoleDesc());
            assertEquals(1, role.getSortOrder());
            assertEquals(RoleType.BUSINESS, role.getRoleType());
            assertEquals(DataScope.SELF, role.getDataScope());
            assertEquals(RoleStatus.NORMAL, role.getStatus());
            assertTrue(role.isActive());
        }
    }

    @Nested
    @DisplayName("createSystemRole")
    class CreateSystemRole {

        @Test
        @DisplayName("should create system role with data scope")
        void shouldCreateSystemRoleWithDataScope() {
            // When
            Role role = Role.createSystemRole("超级管理员", "super_admin", "超级管理员角色",
                0, DataScope.ALL);

            // Then
            assertNotNull(role);
            assertEquals("超级管理员", role.getRoleName());
            assertEquals("super_admin", role.getRoleCode());
            assertEquals(RoleType.SYSTEM, role.getRoleType());
            assertEquals(DataScope.ALL, role.getDataScope());
            assertEquals(RoleStatus.NORMAL, role.getStatus());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update all fields when provided")
        void shouldUpdateAllFieldsWhenProvided() {
            // Given
            Role role = Role.create("管理员", "admin", "旧描述", 1);

            // When
            role.updateInfo("超级管理员", "新描述", 2);

            // Then
            assertEquals("超级管理员", role.getRoleName());
            assertEquals("新描述", role.getRoleDesc());
            assertEquals(2, role.getSortOrder());
        }

        @Test
        @DisplayName("should not update fields when null")
        void shouldNotUpdateFieldsWhenNull() {
            // Given
            Role role = Role.create("管理员", "admin", "描述", 1);

            // When
            role.updateInfo(null, null, null);

            // Then
            assertEquals("管理员", role.getRoleName());
            assertEquals("描述", role.getRoleDesc());
            assertEquals(1, role.getSortOrder());
        }

        @Test
        @DisplayName("should update partial fields")
        void shouldUpdatePartialFields() {
            // Given
            Role role = Role.create("管理员", "admin", "描述", 1);

            // When
            role.updateInfo("新名称", null, 5);

            // Then
            assertEquals("新名称", role.getRoleName());
            assertEquals("描述", role.getRoleDesc());
            assertEquals(5, role.getSortOrder());
        }
    }

    @Nested
    @DisplayName("enable/disable")
    class EnableDisable {

        @Test
        @DisplayName("should disable role")
        void shouldDisableRole() {
            // Given
            Role role = Role.create("管理员", "admin", null, 1);
            assertTrue(role.isActive());

            // When
            role.disable();

            // Then
            assertFalse(role.isActive());
            assertEquals(RoleStatus.DISABLED, role.getStatus());
        }

        @Test
        @DisplayName("should enable role")
        void shouldEnableRole() {
            // Given
            Role role = Role.create("管理员", "admin", null, 1);
            role.disable();
            assertFalse(role.isActive());

            // When
            role.enable();

            // Then
            assertTrue(role.isActive());
            assertEquals(RoleStatus.NORMAL, role.getStatus());
        }
    }

    @Nested
    @DisplayName("setDataScope")
    class SetDataScope {

        @Test
        @DisplayName("should set data scope")
        void shouldSetDataScope() {
            // Given
            Role role = Role.create("管理员", "admin", null, 1);
            assertEquals(DataScope.SELF, role.getDataScope());

            // When
            role.setDataScope(DataScope.ALL);

            // Then
            assertEquals(DataScope.ALL, role.getDataScope());
        }
    }
}

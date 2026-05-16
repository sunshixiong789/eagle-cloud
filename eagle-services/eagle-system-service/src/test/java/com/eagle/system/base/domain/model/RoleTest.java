package com.eagle.system.base.domain.model;

import com.eagle.system.base.domain.model.enums.DataScope;
import com.eagle.system.base.domain.model.enums.RoleStatus;
import com.eagle.system.base.domain.model.enums.RoleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create a business role with default scope SELF and status NORMAL")
        void shouldCreateBusinessRole() {
            Role role = Role.create("管理员", "admin", "管理员角色", 10);

            assertEquals("管理员", role.getRoleName());
            assertEquals("admin", role.getRoleCode());
            assertEquals("管理员角色", role.getRoleDesc());
            assertEquals(10, role.getSortOrder());
            assertEquals(RoleType.BUSINESS, role.getRoleType());
            assertEquals(DataScope.SELF, role.getDataScope());
            assertEquals(RoleStatus.NORMAL, role.getStatus());
            assertTrue(role.isActive());
            assertFalse(role.isSystemRole());
        }
    }

    @Nested
    @DisplayName("createSystemRole")
    class CreateSystemRole {

        @Test
        @DisplayName("should mark as SYSTEM type with supplied data scope")
        void shouldCreateSystemRole() {
            Role role = Role.createSystemRole("超级管理员", "super_admin", "系统内置", 1, DataScope.ALL);
            assertEquals(RoleType.SYSTEM, role.getRoleType());
            assertEquals(DataScope.ALL, role.getDataScope());
            assertTrue(role.isSystemRole());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update only non-null fields")
        void shouldUpdateNonNullFields() {
            Role role = Role.create("旧名", "code", "旧描述", 1);
            role.updateInfo("新名", null, 99);
            assertEquals("新名", role.getRoleName());
            assertEquals("旧描述", role.getRoleDesc());
            assertEquals(99, role.getSortOrder());
        }
    }

    @Nested
    @DisplayName("setDataScope")
    class SetDataScope {

        @Test
        @DisplayName("should replace data scope")
        void shouldReplaceScope() {
            Role role = Role.create("Op", "op", "operator", 5);
            role.setDataScope(DataScope.DEPT_AND_CHILD);
            assertEquals(DataScope.DEPT_AND_CHILD, role.getDataScope());
        }
    }

    @Nested
    @DisplayName("enable / disable")
    class EnableDisable {

        @Test
        @DisplayName("should toggle status with isActive reflecting state")
        void shouldToggle() {
            Role role = Role.create("R", "r", "x", 1);
            role.disable();
            assertEquals(RoleStatus.DISABLED, role.getStatus());
            assertFalse(role.isActive());
            role.enable();
            assertEquals(RoleStatus.NORMAL, role.getStatus());
            assertTrue(role.isActive());
        }
    }
}

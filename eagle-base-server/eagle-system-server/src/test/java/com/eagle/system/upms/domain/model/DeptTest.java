package com.eagle.system.domain.model;

import com.eagle.system.domain.model.enums.DeptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dept 聚合根单元测试
 *
 * @author sunshixiong
 */
@DisplayName("Dept 聚合根")
class DeptTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create dept with all fields")
        void shouldCreateDeptWithAllFields() {
            // When
            Dept dept = Dept.create(1L, "技术部", 100L, "13800000000", 1);

            // Then
            assertNotNull(dept);
            assertEquals(1L, dept.getParentId());
            assertEquals("技术部", dept.getName());
            assertEquals(100L, dept.getLeaderId());
            assertEquals("13800000000", dept.getPhone());
            assertEquals(1, dept.getSortOrder());
            assertEquals(DeptStatus.NORMAL, dept.getStatus());
        }

        @Test
        @DisplayName("should create root dept with null parentId")
        void shouldCreateRootDeptWithNullParentId() {
            // When
            Dept dept = Dept.create(null, "公司", null, null, 0);

            // Then
            assertNotNull(dept);
            assertNull(dept.getParentId());
            assertEquals("公司", dept.getName());
            assertNull(dept.getLeaderId());
            assertNull(dept.getPhone());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update all fields when provided")
        void shouldUpdateAllFieldsWhenProvided() {
            // Given
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);

            // When
            dept.updateInfo("研发部", 2L, "13900000000", 2);

            // Then
            assertEquals("研发部", dept.getName());
            assertEquals(2L, dept.getLeaderId());
            assertEquals("13900000000", dept.getPhone());
            assertEquals(2, dept.getSortOrder());
        }

        @Test
        @DisplayName("should not update fields when null")
        void shouldNotUpdateFieldsWhenNull() {
            // Given
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);

            // When
            dept.updateInfo(null, null, null, null);

            // Then
            assertEquals("技术部", dept.getName());
            assertEquals(1L, dept.getLeaderId());
            assertEquals("13800000000", dept.getPhone());
            assertEquals(1, dept.getSortOrder());
        }

        @Test
        @DisplayName("should update partial fields")
        void shouldUpdatePartialFields() {
            // Given
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);

            // When
            dept.updateInfo("研发部", null, null, 5);

            // Then
            assertEquals("研发部", dept.getName());
            assertEquals(1L, dept.getLeaderId());
            assertEquals("13800000000", dept.getPhone());
            assertEquals(5, dept.getSortOrder());
        }
    }

    @Nested
    @DisplayName("updateLeader")
    class UpdateLeader {

        @Test
        @DisplayName("should update leader")
        void shouldUpdateLeader() {
            // Given
            Dept dept = Dept.create(null, "技术部", 1L, null, 1);

            // When
            dept.updateLeader(2L);

            // Then
            assertEquals(2L, dept.getLeaderId());
        }

        @Test
        @DisplayName("should set leader to null")
        void shouldSetLeaderToNull() {
            // Given
            Dept dept = Dept.create(null, "技术部", 1L, null, 1);

            // When
            dept.updateLeader(null);

            // Then
            assertNull(dept.getLeaderId());
        }
    }

    @Nested
    @DisplayName("setPathAndLevel")
    class SetPathAndLevel {

        @Test
        @DisplayName("should set dept path and level")
        void shouldSetDeptPathAndLevel() {
            // Given
            Dept dept = Dept.create(1L, "前端组", null, null, 1);

            // When
            dept.setPathAndLevel("/1/2/", 2);

            // Then
            assertEquals("/1/2/", dept.getDeptPath());
            assertEquals(2, dept.getLevel());
        }
    }

    @Nested
    @DisplayName("enable/disable")
    class EnableDisable {

        @Test
        @DisplayName("should disable dept")
        void shouldDisableDept() {
            // Given
            Dept dept = Dept.create(null, "技术部", null, null, 1);
            assertEquals(DeptStatus.NORMAL, dept.getStatus());

            // When
            dept.disable();

            // Then
            assertEquals(DeptStatus.DISABLED, dept.getStatus());
        }

        @Test
        @DisplayName("should enable dept")
        void shouldEnableDept() {
            // Given
            Dept dept = Dept.create(null, "技术部", null, null, 1);
            dept.disable();

            // When
            dept.enable();

            // Then
            assertEquals(DeptStatus.NORMAL, dept.getStatus());
        }
    }
}

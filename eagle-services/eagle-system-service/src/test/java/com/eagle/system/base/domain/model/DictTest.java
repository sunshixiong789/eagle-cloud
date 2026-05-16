package com.eagle.system.base.domain.model;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.domain.model.enums.DictStatus;
import com.eagle.system.base.domain.model.enums.DictType;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create dict with ACTIVE status and non-system flag")
        void shouldCreate() {
            Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", "状态字典", "备注");
            assertEquals(DictType.USER_STATUS, dict.getDictType());
            assertEquals("用户状态", dict.getDictName());
            assertEquals(DictStatus.ACTIVE, dict.getStatus());
            assertFalse(dict.isSystem());
            assertTrue(dict.getDictItems().isEmpty());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update only non-null fields")
        void shouldUpdateNonNull() {
            Dict dict = Dict.create(DictType.USER_STATUS, "旧", "旧描述", "旧备注");
            dict.updateInfo("新", null, "新备注");
            assertEquals("新", dict.getDictName());
            assertEquals("旧描述", dict.getDescription());
            assertEquals("新备注", dict.getRemarks());
        }
    }

    @Nested
    @DisplayName("addItem / findItemById / removeItemById")
    class ItemLifecycle {

        @Test
        @DisplayName("should add item and locate it by id")
        void shouldAddAndFind() {
            Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", null, null);
            DictItemEntity item = dict.addItem("ACTIVE", "已激活", 0L, "desc", 1, null);
            ReflectionTestUtils.setField(item, "id", 42L);
            assertNotNull(item);
            assertEquals(1, dict.getDictItems().size());
            assertEquals(item, dict.findItemById(42L));
        }

        @Test
        @DisplayName("should remove item by id")
        void shouldRemoveItem() {
            Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", null, null);
            DictItemEntity item = dict.addItem("V", "L", 0L, null, 1, null);
            ReflectionTestUtils.setField(item, "id", 7L);
            dict.removeItemById(7L);
            assertTrue(dict.getDictItems().isEmpty());
        }

        @Test
        @DisplayName("findItemById should throw NotFoundException when id unknown")
        void shouldThrowWhenItemMissing() {
            Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", null, null);
            AppException ex = assertThrows(NotFoundException.class, () -> dict.findItemById(999L));
            assertEquals(SystemErrorCode.DICT_ITEM_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class ActivateDeactivate {

        @Test
        @DisplayName("should toggle status")
        void shouldToggle() {
            Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", null, null);
            dict.deactivate();
            assertEquals(DictStatus.INACTIVE, dict.getStatus());
            dict.activate();
            assertEquals(DictStatus.ACTIVE, dict.getStatus());
        }
    }

    @Nested
    @DisplayName("DictItemEntity")
    class DictItemEntityTests {

        @Test
        @DisplayName("create should populate with parentId fallback to 0")
        void shouldCreateItemWithDefaults() {
            DictItemEntity item = DictItemEntity.create(1L, "V", "L", DictType.USER_STATUS,
                    null, "d", 5, "r");
            assertEquals(0L, item.getParentId());
            assertEquals(DictStatus.ACTIVE, item.getStatus());
            assertEquals("V", item.getItemValue());
        }

        @Test
        @DisplayName("updateInfo should update only non-null fields")
        void shouldUpdateNonNull() {
            DictItemEntity item = DictItemEntity.create(1L, "V", "L", DictType.USER_STATUS,
                    null, "d", 5, "r");
            item.updateInfo(null, "newLabel", null, 99, null);
            assertEquals("V", item.getItemValue());
            assertEquals("newLabel", item.getName());
            assertEquals(99, item.getSortOrder());
            assertEquals("r", item.getRemarks());
        }

        @Test
        @DisplayName("activate / deactivate toggles status")
        void shouldToggleStatus() {
            DictItemEntity item = DictItemEntity.create(1L, "V", "L", DictType.USER_STATUS,
                    0L, null, 1, null);
            item.deactivate();
            assertEquals(DictStatus.INACTIVE, item.getStatus());
            item.activate();
            assertEquals(DictStatus.ACTIVE, item.getStatus());
        }
    }
}

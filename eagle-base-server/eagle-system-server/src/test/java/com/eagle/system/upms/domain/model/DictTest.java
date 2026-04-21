package com.eagle.system.domain.model;

import com.eagle.system.domain.model.entity.DictItemEntity;
import com.eagle.system.domain.model.enums.DictStatus;
import com.eagle.system.domain.model.enums.DictType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dict 聚合根单元测试
 *
 * @author sunshixiong
 */
@DisplayName("Dict 聚合根")
class DictTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create dict with all fields")
        void shouldCreateDictWithAllFields() {
            // When
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "性别字典", "备注");

            // Then
            assertNotNull(dict);
            assertEquals(DictType.USER_GENDER, dict.getDictType());
            assertEquals("用户性别", dict.getDictName());
            assertEquals("性别字典", dict.getDescription());
            assertEquals("备注", dict.getRemarks());
            assertFalse(dict.isSystem());
            assertEquals(DictStatus.ACTIVE, dict.getStatus());
            assertTrue(dict.getDictItems().isEmpty());
        }

        @Test
        @DisplayName("should create dict with null optional fields")
        void shouldCreateDictWithNullOptionalFields() {
            // When
            Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", null, null);

            // Then
            assertNotNull(dict);
            assertNull(dict.getDescription());
            assertNull(dict.getRemarks());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfo {

        @Test
        @DisplayName("should update all fields when provided")
        void shouldUpdateAllFieldsWhenProvided() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "旧描述", "旧备注");

            // When
            dict.updateInfo("性别", "新描述", "新备注");

            // Then
            assertEquals("性别", dict.getDictName());
            assertEquals("新描述", dict.getDescription());
            assertEquals("新备注", dict.getRemarks());
        }

        @Test
        @DisplayName("should not update fields when null")
        void shouldNotUpdateFieldsWhenNull() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", "备注");

            // When
            dict.updateInfo(null, null, null);

            // Then
            assertEquals("用户性别", dict.getDictName());
            assertEquals("描述", dict.getDescription());
            assertEquals("备注", dict.getRemarks());
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("should add item to dict")
        void shouldAddItemToDict() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);
            setDictId(dict, 1L);

            // When
            DictItemEntity item = dict.addItem("male", "男", 0L, "男性", 1, null);

            // Then
            assertNotNull(item);
            assertEquals(1, dict.getDictItems().size());
            assertEquals("male", item.getItemValue());
            assertEquals("男", item.getName());
            assertEquals(DictType.USER_GENDER, item.getDictType());
        }

        @Test
        @DisplayName("should add multiple items")
        void shouldAddMultipleItems() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);
            setDictId(dict, 1L);

            // When
            dict.addItem("male", "男", 0L, null, 1, null);
            dict.addItem("female", "女", 0L, null, 2, null);

            // Then
            assertEquals(2, dict.getDictItems().size());
        }
    }

    @Nested
    @DisplayName("findItemById")
    class FindItemById {

        @Test
        @DisplayName("should find item by id")
        void shouldFindItemById() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);
            setDictId(dict, 1L);
            DictItemEntity item = dict.addItem("male", "男", 0L, null, 1, null);
            setItemId(item, 10L);

            // When
            DictItemEntity found = dict.findItemById(10L);

            // Then
            assertEquals(item, found);
        }

        @Test
        @DisplayName("should throw when item not found")
        void shouldThrowWhenItemNotFound() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                dict.findItemById(999L));
        }
    }

    @Nested
    @DisplayName("removeItemById")
    class RemoveItemById {

        @Test
        @DisplayName("should remove item by id")
        void shouldRemoveItemById() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);
            setDictId(dict, 1L);
            DictItemEntity item = dict.addItem("male", "男", 0L, null, 1, null);
            setItemId(item, 10L);
            assertEquals(1, dict.getDictItems().size());

            // When
            dict.removeItemById(10L);

            // Then
            assertTrue(dict.getDictItems().isEmpty());
        }

        @Test
        @DisplayName("should throw when removing non-existent item")
        void shouldThrowWhenRemovingNonExistent() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                dict.removeItemById(999L));
        }
    }

    @Nested
    @DisplayName("activate/deactivate")
    class ActivateDeactivate {

        @Test
        @DisplayName("should deactivate dict")
        void shouldDeactivateDict() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);
            assertEquals(DictStatus.ACTIVE, dict.getStatus());

            // When
            dict.deactivate();

            // Then
            assertEquals(DictStatus.INACTIVE, dict.getStatus());
        }

        @Test
        @DisplayName("should activate dict")
        void shouldActivateDict() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);
            dict.deactivate();

            // When
            dict.activate();

            // Then
            assertEquals(DictStatus.ACTIVE, dict.getStatus());
        }
    }

    @Nested
    @DisplayName("isSystem")
    class IsSystem {

        @Test
        @DisplayName("should return false for non-system dict")
        void shouldReturnFalseForNonSystem() {
            // Given
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", null, null);

            // Then
            assertFalse(dict.isSystem());
        }
    }

    private void setDictId(Dict dict, Long id) {
        try {
            java.lang.reflect.Field idField = dict.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(dict, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setItemId(DictItemEntity item, Long id) {
        try {
            java.lang.reflect.Field idField = item.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.eagle.system.application.service;

import com.eagle.common.exception.NotFoundException;
import com.eagle.system.application.mapper.DictItemMapper;
import com.eagle.system.application.mapper.DictMapper;
import com.eagle.system.domain.model.Dict;
import com.eagle.system.domain.model.entity.DictItemEntity;
import com.eagle.system.domain.model.enums.DictType;
import com.eagle.system.domain.repository.DictRepository;
import com.eagle.system.web.dto.request.CreateDictItemRequest;
import com.eagle.system.web.dto.request.CreateDictRequest;
import com.eagle.system.web.dto.request.UpdateDictItemRequest;
import com.eagle.system.web.dto.request.UpdateDictRequest;
import com.eagle.system.web.dto.response.DictItemResponse;
import com.eagle.system.web.dto.response.DictResponse;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DictApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("字典应用服务")
@ExtendWith(MockitoExtension.class)
class DictApplicationServiceTest {

    @Mock
    private DictRepository dictRepository;

    @Mock
    private DictMapper dictMapper;

    @Mock
    private DictItemMapper dictItemMapper;

    @InjectMocks
    private DictApplicationService dictApplicationService;

    @Nested
    @DisplayName("createDict")
    class CreateDict {

        @Test
        @DisplayName("should create dict successfully")
        void shouldCreateDictSuccessfully() {
            // Given
            CreateDictRequest request = new CreateDictRequest();
            request.setDictType("USER_GENDER");
            request.setDictName("用户性别");
            request.setDescription("性别字典");
            request.setRemarks("备注");

            DictResponse expectedResponse = new DictResponse();

            when(dictRepository.save(any(Dict.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dictMapper.toResponse(any(Dict.class))).thenReturn(expectedResponse);

            // When
            DictResponse result = dictApplicationService.createDict(request);

            // Then
            assertNotNull(result);
            verify(dictRepository).save(any(Dict.class));
        }
    }

    @Nested
    @DisplayName("updateDict")
    class UpdateDict {

        @Test
        @DisplayName("should update dict successfully")
        void shouldUpdateDictSuccessfully() {
            // Given
            Long id = 1L;
            UpdateDictRequest request = new UpdateDictRequest();
            request.setDictName("用户性别(新)");
            request.setDescription("更新的描述");
            request.setRemarks("新备注");

            Dict existingDict = Dict.create(DictType.USER_GENDER, "用户性别", "旧描述", "旧备注");
            DictResponse expectedResponse = new DictResponse();

            when(dictRepository.findById(id)).thenReturn(Optional.of(existingDict));
            when(dictRepository.save(any(Dict.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dictMapper.toResponse(any(Dict.class))).thenReturn(expectedResponse);

            // When
            DictResponse result = dictApplicationService.updateDict(id, request);

            // Then
            assertNotNull(result);
            assertEquals("用户性别(新)", existingDict.getDictName());
            verify(dictRepository).save(existingDict);
        }

        @Test
        @DisplayName("should throw NotFoundException when dict not found")
        void shouldThrowWhenDictNotFound() {
            // Given
            Long id = 999L;
            UpdateDictRequest request = new UpdateDictRequest();

            when(dictRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                dictApplicationService.updateDict(id, request));
        }
    }

    @Nested
    @DisplayName("deleteDict")
    class DeleteDict {

        @Test
        @DisplayName("should delete dict successfully")
        void shouldDeleteDictSuccessfully() {
            // Given
            Long id = 1L;
            when(dictRepository.existsById(id)).thenReturn(true);

            // When
            dictApplicationService.deleteDict(id);

            // Then
            verify(dictRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw NotFoundException when dict not found")
        void shouldThrowWhenDictNotFound() {
            // Given
            Long id = 999L;
            when(dictRepository.existsById(id)).thenReturn(false);

            // When & Then
            assertThrows(NotFoundException.class, () ->
                dictApplicationService.deleteDict(id));
            verify(dictRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getDictById")
    class GetDictById {

        @Test
        @DisplayName("should return dict response when found")
        void shouldReturnDictResponse() {
            // Given
            Long id = 1L;
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            DictResponse expectedResponse = new DictResponse();

            when(dictRepository.findById(id)).thenReturn(Optional.of(dict));
            when(dictMapper.toResponse(dict)).thenReturn(expectedResponse);

            // When
            DictResponse result = dictApplicationService.getDictById(id);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw NotFoundException when dict not found")
        void shouldThrowWhenDictNotFound() {
            // Given
            Long id = 999L;
            when(dictRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                dictApplicationService.getDictById(id));
        }
    }

    @Nested
    @DisplayName("getDictByType")
    class GetDictByType {

        @Test
        @DisplayName("should return dict with items tree by type")
        void shouldReturnDictWithItemsTree() {
            // Given
            String dictType = "USER_GENDER";
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            DictResponse expectedResponse = new DictResponse();

            when(dictRepository.findByDictType(DictType.USER_GENDER)).thenReturn(Optional.of(dict));
            when(dictMapper.toResponse(dict)).thenReturn(expectedResponse);

            // When
            DictResponse result = dictApplicationService.getDictByType(dictType);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw NotFoundException when dict type not found")
        void shouldThrowWhenDictTypeNotFound() {
            // Given
            String dictType = "USER_GENDER";
            when(dictRepository.findByDictType(DictType.USER_GENDER)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                dictApplicationService.getDictByType(dictType));
        }
    }

    @Nested
    @DisplayName("getDictByTypes")
    class GetDictByTypes {

        @Test
        @DisplayName("should return multiple dicts by types")
        void shouldReturnMultipleDicts() {
            // Given
            List<String> types = List.of("USER_GENDER", "USER_STATUS");
            Dict dict1 = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            Dict dict2 = Dict.create(DictType.USER_STATUS, "用户状态", "描述", null);
            DictResponse response1 = new DictResponse();
            DictResponse response2 = new DictResponse();

            when(dictRepository.findByDictTypeIn(any())).thenReturn(List.of(dict1, dict2));
            when(dictMapper.toResponse(dict1)).thenReturn(response1);
            when(dictMapper.toResponse(dict2)).thenReturn(response2);

            // When
            List<DictResponse> result = dictApplicationService.getDictByTypes(types);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("queryDict")
    class QueryDict {

        @Test
        @DisplayName("should return paginated dicts")
        void shouldReturnPaginatedDicts() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            Page<Dict> dictPage = new PageImpl<>(List.of(dict));
            DictResponse response = new DictResponse();

            when(dictRepository.findAll(pageable)).thenReturn(dictPage);
            when(dictMapper.toResponse(dict)).thenReturn(response);

            // When
            Page<DictResponse> result = dictApplicationService.queryDict(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("activateDict")
    class ActivateDict {

        @Test
        @DisplayName("should activate dict successfully")
        void shouldActivateDictSuccessfully() {
            // Given
            Long id = 1L;
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            dict.deactivate();

            when(dictRepository.findById(id)).thenReturn(Optional.of(dict));

            // When
            dictApplicationService.activateDict(id);

            // Then
            verify(dictRepository).save(dict);
        }

        @Test
        @DisplayName("should throw NotFoundException when dict not found")
        void shouldThrowWhenDictNotFound() {
            // Given
            Long id = 999L;
            when(dictRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                dictApplicationService.activateDict(id));
        }
    }

    @Nested
    @DisplayName("deactivateDict")
    class DeactivateDict {

        @Test
        @DisplayName("should deactivate dict successfully")
        void shouldDeactivateDictSuccessfully() {
            // Given
            Long id = 1L;
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);

            when(dictRepository.findById(id)).thenReturn(Optional.of(dict));

            // When
            dictApplicationService.deactivateDict(id);

            // Then
            verify(dictRepository).save(dict);
        }
    }

    @Nested
    @DisplayName("createDictItem")
    class CreateDictItem {

        @Test
        @DisplayName("should create dict item successfully")
        void shouldCreateDictItemSuccessfully() {
            // Given
            Long dictId = 1L;
            CreateDictItemRequest request = new CreateDictItemRequest();
            request.setItemValue("male");
            request.setName("男");
            request.setParentId(0L);
            request.setDescription("男性");
            request.setSortOrder(1);
            request.setRemarks(null);

            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);
            DictItemResponse expectedResponse = new DictItemResponse();

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));
            when(dictRepository.save(any(Dict.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dictItemMapper.toResponse(any(DictItemEntity.class))).thenReturn(expectedResponse);

            // When
            DictItemResponse result = dictApplicationService.createDictItem(dictId, request);

            // Then
            assertNotNull(result);
            assertEquals(1, dict.getDictItems().size());
            verify(dictRepository).save(dict);
        }

        @Test
        @DisplayName("should throw NotFoundException when dict not found")
        void shouldThrowWhenDictNotFound() {
            // Given
            Long dictId = 999L;
            CreateDictItemRequest request = new CreateDictItemRequest();

            when(dictRepository.findById(dictId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                dictApplicationService.createDictItem(dictId, request));
        }
    }

    @Nested
    @DisplayName("updateDictItem")
    class UpdateDictItem {

        @Test
        @DisplayName("should update dict item successfully")
        void shouldUpdateDictItemSuccessfully() {
            // Given
            Long dictId = 1L;
            Long itemId = 10L;
            UpdateDictItemRequest request = new UpdateDictItemRequest();
            request.setItemValue("female");
            request.setName("女");
            request.setDescription("女性");
            request.setSortOrder(2);
            request.setRemarks("更新");

            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);
            DictItemEntity item = DictItemEntity.create(dictId, "male", "男", DictType.USER_GENDER, 0L, "男性", 1, null);
            setItemId(item, itemId);
            dict.getDictItems().add(item);
            DictItemResponse expectedResponse = new DictItemResponse();

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));
            when(dictRepository.save(any(Dict.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dictItemMapper.toResponse(any(DictItemEntity.class))).thenReturn(expectedResponse);

            // When
            DictItemResponse result = dictApplicationService.updateDictItem(dictId, itemId, request);

            // Then
            assertNotNull(result);
            assertEquals("female", item.getItemValue());
            assertEquals("女", item.getName());
            verify(dictRepository).save(dict);
        }
    }

    @Nested
    @DisplayName("deleteDictItem")
    class DeleteDictItem {

        @Test
        @DisplayName("should delete dict item successfully")
        void shouldDeleteDictItemSuccessfully() {
            // Given
            Long dictId = 1L;
            Long itemId = 10L;

            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);
            DictItemEntity item = DictItemEntity.create(dictId, "male", "男", DictType.USER_GENDER, 0L, "男性", 1, null);
            setItemId(item, itemId);
            dict.getDictItems().add(item);

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));

            // When
            dictApplicationService.deleteDictItem(dictId, itemId);

            // Then
            assertTrue(dict.getDictItems().isEmpty());
            verify(dictRepository).save(dict);
        }
    }

    @Nested
    @DisplayName("getDictItemById")
    class GetDictItemById {

        @Test
        @DisplayName("should return dict item response when found")
        void shouldReturnDictItemResponse() {
            // Given
            Long dictId = 1L;
            Long itemId = 10L;

            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);
            DictItemEntity item = DictItemEntity.create(dictId, "male", "男", DictType.USER_GENDER, 0L, "男性", 1, null);
            setItemId(item, itemId);
            dict.getDictItems().add(item);
            DictItemResponse expectedResponse = new DictItemResponse();

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));
            when(dictItemMapper.toResponse(item)).thenReturn(expectedResponse);

            // When
            DictItemResponse result = dictApplicationService.getDictItemById(dictId, itemId);

            // Then
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("queryDictItems")
    class QueryDictItems {

        @Test
        @DisplayName("should return dict items tree")
        void shouldReturnDictItemsTree() {
            // Given
            Long dictId = 1L;
            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);

            DictItemEntity item1 = DictItemEntity.create(dictId, "male", "男", DictType.USER_GENDER, 0L, null, 1, null);
            setItemId(item1, 10L);
            DictItemEntity item2 = DictItemEntity.create(dictId, "female", "女", DictType.USER_GENDER, 0L, null, 2, null);
            setItemId(item2, 11L);
            dict.getDictItems().add(item1);
            dict.getDictItems().add(item2);

            DictItemResponse resp1 = new DictItemResponse();
            resp1.setChildren(new java.util.ArrayList<>());
            DictItemResponse resp2 = new DictItemResponse();
            resp2.setChildren(new java.util.ArrayList<>());

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));
            when(dictItemMapper.toResponse(item1)).thenReturn(resp1);
            when(dictItemMapper.toResponse(item2)).thenReturn(resp2);

            // When
            List<DictItemResponse> result = dictApplicationService.queryDictItems(dictId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("activateDictItem")
    class ActivateDictItem {

        @Test
        @DisplayName("should activate dict item successfully")
        void shouldActivateDictItemSuccessfully() {
            // Given
            Long dictId = 1L;
            Long itemId = 10L;

            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);
            DictItemEntity item = DictItemEntity.create(dictId, "male", "男", DictType.USER_GENDER, 0L, null, 1, null);
            setItemId(item, itemId);
            item.deactivate();
            dict.getDictItems().add(item);

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));

            // When
            dictApplicationService.activateDictItem(dictId, itemId);

            // Then
            verify(dictRepository).save(dict);
        }
    }

    @Nested
    @DisplayName("deactivateDictItem")
    class DeactivateDictItem {

        @Test
        @DisplayName("should deactivate dict item successfully")
        void shouldDeactivateDictItemSuccessfully() {
            // Given
            Long dictId = 1L;
            Long itemId = 10L;

            Dict dict = Dict.create(DictType.USER_GENDER, "用户性别", "描述", null);
            setDictId(dict, dictId);
            DictItemEntity item = DictItemEntity.create(dictId, "male", "男", DictType.USER_GENDER, 0L, null, 1, null);
            setItemId(item, itemId);
            dict.getDictItems().add(item);

            when(dictRepository.findById(dictId)).thenReturn(Optional.of(dict));

            // When
            dictApplicationService.deactivateDictItem(dictId, itemId);

            // Then
            verify(dictRepository).save(dict);
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

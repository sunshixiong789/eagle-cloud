package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.base.application.mapper.DictItemMapper;
import com.eagle.system.base.application.mapper.DictMapper;
import com.eagle.system.base.domain.model.Dict;
import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.domain.model.enums.DictType;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import com.eagle.system.base.domain.repository.DictRepository;
import com.eagle.system.base.interfaces.dto.request.CreateDictItemRequest;
import com.eagle.system.base.interfaces.dto.request.CreateDictRequest;
import com.eagle.system.base.interfaces.dto.response.DictItemResponse;
import com.eagle.system.base.interfaces.dto.response.DictResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictApplicationServiceTest {

    private static final Long DICT_ID = 10L;

    @Mock DictRepository dictRepository;
    @Mock DictMapper dictMapper;
    @Mock DictItemMapper dictItemMapper;
    @InjectMocks DictApplicationService service;

    private Dict sampleDict() {
        Dict dict = Dict.create(DictType.USER_STATUS, "用户状态", null, null);
        ReflectionTestUtils.setField(dict, "id", DICT_ID);
        return dict;
    }

    @Nested
    @DisplayName("createDict")
    class Create {
        @Test
        @DisplayName("should create dict with type from request")
        void shouldCreate() {
            CreateDictRequest req = new CreateDictRequest();
            req.setDictType("USER_STATUS");
            req.setDictName("用户状态");
            Dict saved = sampleDict();
            when(dictRepository.save(any(Dict.class))).thenReturn(saved);
            when(dictMapper.toResponse(saved)).thenReturn(new DictResponse());

            service.createDict(req);

            verify(dictRepository).save(any(Dict.class));
        }
    }

    @Nested
    @DisplayName("deleteDict")
    class Delete {
        @Test
        @DisplayName("should throw when dict missing")
        void shouldThrowWhenMissing() {
            when(dictRepository.existsById(DICT_ID)).thenReturn(false);
            AppException ex = assertThrows(NotFoundException.class, () -> service.deleteDict(DICT_ID));
            assertEquals(SystemErrorCode.DICT_NOT_FOUND, ex.getErrorCode());
            verify(dictRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should delete when exists")
        void shouldDelete() {
            when(dictRepository.existsById(DICT_ID)).thenReturn(true);
            service.deleteDict(DICT_ID);
            verify(dictRepository).deleteById(DICT_ID);
        }
    }

    @Nested
    @DisplayName("getDictById")
    class GetById {
        @Test
        @DisplayName("should throw when dict missing")
        void shouldThrowWhenMissing() {
            when(dictRepository.findById(DICT_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.getDictById(DICT_ID));
        }
    }

    @Nested
    @DisplayName("createDictItem")
    class CreateItem {
        @Test
        @DisplayName("should add item via aggregate and persist")
        void shouldAddItem() {
            Dict dict = sampleDict();
            when(dictRepository.findById(DICT_ID)).thenReturn(Optional.of(dict));
            when(dictRepository.save(dict)).thenReturn(dict);
            when(dictItemMapper.toResponse(any(DictItemEntity.class))).thenReturn(new DictItemResponse());

            CreateDictItemRequest req = new CreateDictItemRequest();
            req.setItemValue("ACTIVE");
            req.setName("已激活");
            req.setParentId(0L);
            req.setSortOrder(1);

            service.createDictItem(DICT_ID, req);
            assertEquals(1, dict.getDictItems().size());
            verify(dictRepository).save(dict);
        }
    }
}

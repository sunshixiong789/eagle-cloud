package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.DictItemMapper;
import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.domain.model.enums.DictType;
import com.eagle.system.base.domain.repository.DictItemRepository;
import com.eagle.system.base.web.dto.request.CreateDictItemRequest;
import com.eagle.system.base.web.dto.request.UpdateDictItemRequest;
import com.eagle.system.base.web.dto.response.DictItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictItemApplicationService {

    private final DictItemRepository dictItemRepository;
    private final DictItemMapper dictItemMapper;

    @Transactional(rollbackFor = Exception.class)
    public DictItemResponse createDictItem(CreateDictItemRequest request) {
        DictItemEntity item = DictItemEntity.create(
                request.getDictId(),
                request.getItemValue(),
                request.getName(),
                DictType.valueOf(request.getDictType()),
                request.getParentId(),
                request.getDescription(),
                request.getSortOrder(),
                request.getRemarks()
        );
        DictItemEntity saved = dictItemRepository.save(item);
        return dictItemMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public DictItemResponse updateDictItem(Long id, UpdateDictItemRequest request) {
        DictItemEntity item = dictItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在"));
        item.updateInfo(
                request.getItemValue(),
                request.getName(),
                request.getDescription(),
                request.getSortOrder(),
                request.getRemarks()
        );
        DictItemEntity saved = dictItemRepository.save(item);
        return dictItemMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDictItem(Long id) {
        dictItemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DictItemResponse getDictItemById(Long id) {
        DictItemEntity item = dictItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在"));
        return dictItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<DictItemResponse> queryDictItemsByDictId(Long dictId) {
        List<DictItemEntity> allItems = dictItemRepository.findByDictId(dictId);
        return buildDictItemTree(allItems, 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activateDictItem(Long id) {
        DictItemEntity item = dictItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在"));
        item.activate();
        dictItemRepository.save(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deactivateDictItem(Long id) {
        DictItemEntity item = dictItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在"));
        item.deactivate();
        dictItemRepository.save(item);
    }

    private List<DictItemResponse> buildDictItemTree(List<DictItemEntity> allItems, Long parentId) {
        return allItems.stream()
                .filter(item -> Objects.equals(item.getParentId(), parentId))
                .sorted(Comparator.comparing(DictItemEntity::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> {
                    DictItemResponse response = dictItemMapper.toResponse(item);
                    response.setChildren(buildDictItemTree(allItems, item.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }
}

package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.DictItemMapper;
import com.eagle.system.base.application.mapper.DictMapper;
import com.eagle.system.base.domain.model.Dict;
import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.domain.model.enums.DictType;
import com.eagle.system.base.domain.repository.DictRepository;
import com.eagle.system.base.web.dto.request.CreateDictItemRequest;
import com.eagle.system.base.web.dto.request.CreateDictRequest;
import com.eagle.system.base.web.dto.request.UpdateDictItemRequest;
import com.eagle.system.base.web.dto.request.UpdateDictRequest;
import com.eagle.system.base.web.dto.response.DictItemResponse;
import com.eagle.system.base.web.dto.response.DictResponse;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 字典应用服务
 * <p>
 * 管理字典聚合根及其子实体（字典项）的所有操作。
 * 字典项的增删改通过 Dict 聚合根进行，遵循 DDD 聚合边界。
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class DictApplicationService {

    private final DictRepository dictRepository;
    private final DictMapper dictMapper;
    private final DictItemMapper dictItemMapper;

    // ==================== 字典操作 ====================

    /**
     * 创建字典
     */
    @Transactional(rollbackFor = Exception.class)
    public DictResponse createDict(CreateDictRequest request) {
        DictType dictType = DictType.valueOf(request.getDictType());
        Dict dict = Dict.create(
                dictType,
                request.getDictName(),
                request.getDescription(),
                request.getRemarks()
        );

        Dict saved = dictRepository.save(dict);
        return dictMapper.toResponse(saved);
    }

    /**
     * 更新字典
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public DictResponse updateDict(Long id, UpdateDictRequest request) {
        Dict dict = findDictById(id);
        dict.updateInfo(
                request.getDictName(),
                request.getDescription(),
                request.getRemarks()
        );

        Dict saved = dictRepository.save(dict);
        return dictMapper.toResponse(saved);
    }

    /**
     * 删除字典（级联删除所有字典项）
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deleteDict(Long id) {
        if (!dictRepository.existsById(id)) {
            throw SystemErrorCode.DICT_NOT_FOUND.toNotFoundException();
        }
        dictRepository.deleteById(id);
    }

    /**
     * 查询单个字典
     */
    @Transactional(readOnly = true)
    public DictResponse getDictById(Long id) {
        Dict dict = findDictById(id);
        return dictMapper.toResponse(dict);
    }

    /**
     * 根据字典类型查询（含字典项树）
     * <p>
     * 供其他服务调用，结果缓存。字典或字典项变更时缓存自动失效。
     *
     * @param dictType 字典类型枚举名
     * @return 字典响应（含字典项树）
     */
    @Cacheable(value = "DICT_TYPE", key = "#dictType")
    @Transactional(readOnly = true)
    public DictResponse getDictByType(String dictType) {
        DictType type = DictType.valueOf(dictType);
        Dict dict = dictRepository.findByDictType(type)
                .orElseThrow(SystemErrorCode.DICT_NOT_FOUND::toNotFoundException);
        DictResponse response = dictMapper.toResponse(dict);
        response.setItems(buildDictItemTree(dict.getDictItems(), 0L));
        return response;
    }

    /**
     * 根据多个字典类型批量查询（含字典项树）
     * <p>
     * 前端一次性加载多个字典，减少请求次数。
     *
     * @param dictTypes 字典类型列表，逗号分隔或集合
     * @return 字典响应列表（含字典项树）
     */
    @Cacheable(value = "DICT_TYPES")
    @Transactional(readOnly = true)
    public List<DictResponse> getDictByTypes(List<String> dictTypes) {
        List<DictType> types = dictTypes.stream()
                .map(DictType::valueOf)
                .toList();
        List<Dict> list = dictRepository.findByDictTypeIn(types);
        return list.stream().map(dict -> {
            DictResponse response = dictMapper.toResponse(dict);
            response.setItems(buildDictItemTree(dict.getDictItems(), 0L));
            return response;
        }).toList();
    }

    /**
     * 分页查询字典列表
     */
    @Transactional(readOnly = true)
    public Page<DictResponse> queryDict(Pageable pageable) {
        return dictRepository.findAll(pageable).map(dictMapper::toResponse);
    }

    /**
     * 激活字典
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void activateDict(Long id) {
        Dict dict = findDictById(id);
        dict.activate();
        dictRepository.save(dict);
    }

    /**
     * 停用字典
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deactivateDict(Long id) {
        Dict dict = findDictById(id);
        dict.deactivate();
        dictRepository.save(dict);
    }

    // ==================== 字典项操作（通过聚合根）====================

    /**
     * 添加字典项
     *
     * @param dictId  字典 ID
     * @param request 创建字典项请求
     * @return 字典项响应
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public DictItemResponse createDictItem(Long dictId, CreateDictItemRequest request) {
        Dict dict = findDictById(dictId);

        DictItemEntity item = dict.addItem(
                request.getItemValue(),
                request.getName(),
                request.getParentId(),
                request.getDescription(),
                request.getSortOrder(),
                request.getRemarks()
        );

        dictRepository.save(dict);
        return dictItemMapper.toResponse(item);
    }

    /**
     * 更新字典项
     *
     * @param dictId  字典 ID
     * @param itemId  字典项 ID
     * @param request 更新字典项请求
     * @return 字典项响应
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public DictItemResponse updateDictItem(Long dictId, Long itemId, UpdateDictItemRequest request) {
        Dict dict = findDictById(dictId);
        DictItemEntity item = dict.findItemById(itemId);

        item.updateInfo(
                request.getItemValue(),
                request.getName(),
                request.getDescription(),
                request.getSortOrder(),
                request.getRemarks()
        );

        dictRepository.save(dict);
        return dictItemMapper.toResponse(item);
    }

    /**
     * 删除字典项
     *
     * @param dictId 字典 ID
     * @param itemId 字典项 ID
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictItem(Long dictId, Long itemId) {
        Dict dict = findDictById(dictId);
        dict.removeItemById(itemId);
        dictRepository.save(dict);
    }

    /**
     * 查询单个字典项
     *
     * @param dictId 字典 ID
     * @param itemId 字典项 ID
     * @return 字典项响应
     */
    @Transactional(readOnly = true)
    public DictItemResponse getDictItemById(Long dictId, Long itemId) {
        Dict dict = findDictById(dictId);
        DictItemEntity item = dict.findItemById(itemId);
        return dictItemMapper.toResponse(item);
    }

    /**
     * 查询字典下的字典项树
     *
     * @param dictId 字典 ID
     * @return 字典项树形列表
     */
    @Transactional(readOnly = true)
    public List<DictItemResponse> queryDictItems(Long dictId) {
        Dict dict = findDictById(dictId);
        return buildDictItemTree(dict.getDictItems(), 0L);
    }

    /**
     * 激活字典项
     *
     * @param dictId 字典 ID
     * @param itemId 字典项 ID
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void activateDictItem(Long dictId, Long itemId) {
        Dict dict = findDictById(dictId);
        DictItemEntity item = dict.findItemById(itemId);
        item.activate();
        dictRepository.save(dict);
    }

    /**
     * 停用字典项
     *
     * @param dictId 字典 ID
     * @param itemId 字典项 ID
     */
    @Caching(evict = {
            @CacheEvict(value = "DICT_TYPE", allEntries = true),
            @CacheEvict(value = "DICT_TYPES", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deactivateDictItem(Long dictId, Long itemId) {
        Dict dict = findDictById(dictId);
        DictItemEntity item = dict.findItemById(itemId);
        item.deactivate();
        dictRepository.save(dict);
    }

    // ==================== 私有方法 ====================

    private Dict findDictById(Long id) {
        return dictRepository.findById(id)
                .orElseThrow(SystemErrorCode.DICT_NOT_FOUND::toNotFoundException);
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

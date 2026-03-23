package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.DictMapper;
import com.eagle.system.base.domain.model.Dict;
import com.eagle.system.base.domain.model.enums.DictType;
import com.eagle.system.base.domain.repository.DictRepository;
import com.eagle.system.base.web.dto.request.CreateDictRequest;
import com.eagle.system.base.web.dto.request.UpdateDictRequest;
import com.eagle.system.base.web.dto.response.DictResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DictApplicationService {

    private final DictRepository dictRepository;
    private final DictMapper dictMapper;

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

    @Transactional(rollbackFor = Exception.class)
    public DictResponse updateDict(Long id, UpdateDictRequest request) {
        Dict dict = dictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典不存在"));

        dict.updateInfo(
                request.getDictName(),
                request.getDescription(),
                request.getRemarks()
        );

        Dict saved = dictRepository.save(dict);
        return dictMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDict(Long id) {
        dictRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DictResponse getDictById(Long id) {
        Dict dict = dictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典不存在"));
        return dictMapper.toResponse(dict);
    }

    @Transactional(readOnly = true)
    public Page<DictResponse> queryDicts(Pageable pageable) {
        return dictRepository.findAll(pageable).map(dictMapper::toResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activateDict(Long id) {
        Dict dict = dictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典不存在"));
        dict.activate();
        dictRepository.save(dict);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deactivateDict(Long id) {
        Dict dict = dictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字典不存在"));
        dict.deactivate();
        dictRepository.save(dict);
    }
}

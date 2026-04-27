package com.eagle.system.base.application.service;

import com.eagle.system.common.exception.SystemErrorCode;
import com.eagle.system.base.application.mapper.DeptMapper;
import com.eagle.system.base.domain.model.Dept;
import com.eagle.system.base.domain.repository.DeptRepository;
import com.eagle.system.base.web.dto.request.CreateDeptRequest;
import com.eagle.system.base.web.dto.request.UpdateDeptRequest;
import com.eagle.system.base.web.dto.response.DeptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptApplicationService {

    private final DeptRepository deptRepository;
    private final DeptMapper deptMapper;

    @CacheEvict(value = "DEPT_TREE", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public DeptResponse createDept(CreateDeptRequest request) {
        Dept dept = Dept.create(
                request.getParentId(),
                request.getName(),
                request.getLeaderId(),
                request.getPhone(),
                request.getSortOrder()
        );

        Dept saved = deptRepository.save(dept);
        return deptMapper.toResponse(saved);
    }

    @CacheEvict(value = "DEPT_TREE", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public DeptResponse updateDept(Long id, UpdateDeptRequest request) {
        Dept dept = findDeptById(id);

        dept.updateInfo(
                request.getName(),
                request.getLeaderId(),
                request.getPhone(),
                request.getSortOrder()
        );

        Dept saved = deptRepository.save(dept);
        return deptMapper.toResponse(saved);
    }

    @CacheEvict(value = "DEPT_TREE", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        deptRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DeptResponse getDeptById(Long id) {
        Dept dept = findDeptById(id);
        return deptMapper.toResponse(dept);
    }

    @Transactional(readOnly = true)
    public Page<DeptResponse> queryDepts(Pageable pageable) {
        return deptRepository.findAll(pageable).map(deptMapper::toResponse);
    }

    @Cacheable(value = "DEPT_TREE")
    @Transactional(readOnly = true)
    public List<DeptResponse> queryDeptTree() {
        List<Dept> allDepts = deptRepository.findAll();
        return buildDeptTree(allDepts, null);
    }

    @CacheEvict(value = "DEPT_TREE", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void enableDept(Long id) {
        Dept dept = findDeptById(id);
        dept.enable();
        deptRepository.save(dept);
    }

    @CacheEvict(value = "DEPT_TREE", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void disableDept(Long id) {
        Dept dept = findDeptById(id);
        dept.disable();
        deptRepository.save(dept);
    }

    private Dept findDeptById(Long id) {
        return deptRepository.findById(id)
                .orElseThrow(SystemErrorCode.DEPT_NOT_FOUND::toNotFoundException);
    }

    /**
     * 构建部门树形结构（O(n) 算法）
     * <p>
     * 先按 parentId 分组建立 Map，再递归设置 children，
     * 避免每层都全量过滤导致的 O(n²) 问题。
     */
    private List<DeptResponse> buildDeptTree(List<Dept> allDepts, Long parentId) {
        // 按 parentId 分组，一次遍历完成，O(n)
        Map<Long, List<Dept>> childrenByParent = new HashMap<>();
        for (Dept dept : allDepts) {
            childrenByParent
                    .computeIfAbsent(dept.getParentId(), k -> new ArrayList<>())
                    .add(dept);
        }
        return buildChildren(childrenByParent, parentId);
    }

    private List<DeptResponse> buildChildren(Map<Long, List<Dept>> childrenByParent, Long parentId) {
        List<Dept> children = childrenByParent.getOrDefault(parentId, Collections.emptyList());
        return children.stream()
                .sorted(Comparator.comparing(
                        Dept::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(dept -> {
                    DeptResponse response = deptMapper.toResponse(dept);
                    response.setChildren(buildChildren(childrenByParent, dept.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }
}

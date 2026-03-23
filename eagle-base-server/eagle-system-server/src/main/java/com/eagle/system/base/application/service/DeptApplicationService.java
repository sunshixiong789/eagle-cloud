package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.DeptMapper;
import com.eagle.system.base.domain.model.Dept;
import com.eagle.system.base.domain.repository.DeptRepository;
import com.eagle.system.base.web.dto.request.CreateDeptRequest;
import com.eagle.system.base.web.dto.request.UpdateDeptRequest;
import com.eagle.system.base.web.dto.response.DeptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeptApplicationService {

    private final DeptRepository deptRepository;
    private final DeptMapper deptMapper;

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

    @Transactional(rollbackFor = Exception.class)
    public DeptResponse updateDept(Long id, UpdateDeptRequest request) {
        Dept dept = deptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("部门不存在"));

        dept.updateInfo(
                request.getName(),
                request.getLeaderId(),
                request.getPhone(),
                request.getSortOrder()
        );

        Dept saved = deptRepository.save(dept);
        return deptMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        deptRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DeptResponse getDeptById(Long id) {
        Dept dept = deptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("部门不存在"));
        return deptMapper.toResponse(dept);
    }

    @Transactional(readOnly = true)
    public Page<DeptResponse> queryDepts(Pageable pageable) {
        return deptRepository.findAll(pageable).map(deptMapper::toResponse);
    }
}

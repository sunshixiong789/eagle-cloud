package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.DeptApplicationService;
import com.eagle.system.base.web.dto.request.CreateDeptRequest;
import com.eagle.system.base.web.dto.request.UpdateDeptRequest;
import com.eagle.system.base.web.dto.response.DeptResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptApplicationService deptApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeptResponse createDept(@Valid @RequestBody CreateDeptRequest request) {
        return deptApplicationService.createDept(request);
    }

    @PutMapping("/{id}")
    public DeptResponse updateDept(@PathVariable Long id, @Valid @RequestBody UpdateDeptRequest request) {
        return deptApplicationService.updateDept(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDept(@PathVariable Long id) {
        deptApplicationService.deleteDept(id);
    }

    @GetMapping("/{id}")
    public DeptResponse getDeptById(@PathVariable Long id) {
        return deptApplicationService.getDeptById(id);
    }

    @GetMapping
    public Page<DeptResponse> queryDepts(Pageable pageable) {
        return deptApplicationService.queryDepts(pageable);
    }
}

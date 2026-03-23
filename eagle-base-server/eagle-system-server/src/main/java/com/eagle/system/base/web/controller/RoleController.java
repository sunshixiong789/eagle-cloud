package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.RoleApplicationService;
import com.eagle.system.base.web.dto.request.CreateRoleRequest;
import com.eagle.system.base.web.dto.request.UpdateRoleRequest;
import com.eagle.system.base.web.dto.response.RoleResponse;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleApplicationService.createRole(request);
    }

    @PutMapping("/{id}")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleApplicationService.updateRole(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long id) {
        roleApplicationService.deleteRole(id);
    }

    @GetMapping("/{id}")
    public RoleResponse getRoleById(@PathVariable Long id) {
        return roleApplicationService.getRoleById(id);
    }

    @GetMapping
    public Page<RoleResponse> queryRoles(Pageable pageable) {
        return roleApplicationService.queryRoles(pageable);
    }
}

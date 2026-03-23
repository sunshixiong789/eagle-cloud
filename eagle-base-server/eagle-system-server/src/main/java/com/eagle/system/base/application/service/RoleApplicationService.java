package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.RoleMapper;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.web.dto.request.CreateRoleRequest;
import com.eagle.system.base.web.dto.request.UpdateRoleRequest;
import com.eagle.system.base.web.dto.response.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional(rollbackFor = Exception.class)
    public RoleResponse createRole(CreateRoleRequest request) {
        Role role = Role.create(
                request.getRoleName(),
                request.getRoleCode(),
                request.getRoleDesc(),
                request.getSortOrder()
        );

        Role saved = roleRepository.save(role);
        return roleMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        role.updateInfo(
                request.getRoleName(),
                request.getRoleDesc(),
                request.getSortOrder()
        );

        Role saved = roleRepository.save(role);
        return roleMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        return roleMapper.toResponse(role);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> queryRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(roleMapper::toResponse);
    }
}

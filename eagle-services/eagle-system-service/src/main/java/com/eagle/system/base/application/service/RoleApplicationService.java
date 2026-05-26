package com.eagle.system.base.application.service;

import com.eagle.audit.annotation.AuditLog;
import com.eagle.system.base.application.mapper.RoleMapper;
import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.infrastructure.persistence.RoleSpecification;
import com.eagle.system.base.interfaces.dto.request.CreateRoleRequest;
import com.eagle.system.base.interfaces.dto.request.RoleQueryRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateRoleRequest;
import com.eagle.system.base.interfaces.dto.response.RoleResponse;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 角色应用服务，负责角色的增删改查、启用/禁用及按角色查询用户等用例编排。
 */
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

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

    @AuditLog(module = "角色管理", action = "更新角色")
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = findRoleById(id);
        if (role.isSystemRole()) {
            throw SystemErrorCode.ROLE_SYSTEM_PROTECTED.toDomainException();
        }

        role.updateInfo(
                request.getRoleName(),
                request.getRoleDesc(),
                request.getSortOrder()
        );

        Role saved = roleRepository.save(role);
        return roleMapper.toResponse(saved);
    }

    @AuditLog(module = "角色管理", action = "删除角色")
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = findRoleById(id);
        if (role.isSystemRole()) {
            throw SystemErrorCode.ROLE_SYSTEM_PROTECTED.toDomainException();
        }
        roleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = findRoleById(id);
        return roleMapper.toResponse(role);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> listRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(roleMapper::toResponse);
    }

    /**
     * 条件查询角色（支持名称模糊、标识精确、状态过滤）
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    @Transactional(readOnly = true)
    public Page<RoleResponse> queryRoles(RoleQueryRequest request, Pageable pageable) {
        Specification<Role> spec = Specification
                .where(RoleSpecification.roleNameLike(request.getRoleName()))
                .and(RoleSpecification.roleCodeEquals(request.getRoleCode()))
                .and(RoleSpecification.statusEquals(request.getStatus()));
        return roleRepository.findAll(spec, pageable).map(roleMapper::toResponse);
    }

    /**
     * 查询拥有指定角色的用户列表
     *
     * @param roleId   角色 ID
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRoleId(Long roleId, Pageable pageable) {
        roleRepository.findById(roleId)
                .orElseThrow(SystemErrorCode.ROLE_NOT_FOUND::toNotFoundException);
        return userRepository.findByRoleId(roleId, pageable).map(userMapper::toResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enableRole(Long id) {
        Role role = findRoleById(id);
        role.enable();
        roleRepository.save(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableRole(Long id) {
        Role role = findRoleById(id);
        if (role.isSystemRole()) {
            throw SystemErrorCode.ROLE_SYSTEM_PROTECTED.toDomainException();
        }
        role.disable();
        roleRepository.save(role);
    }

    private Role findRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(SystemErrorCode.ROLE_NOT_FOUND::toNotFoundException);
    }
}

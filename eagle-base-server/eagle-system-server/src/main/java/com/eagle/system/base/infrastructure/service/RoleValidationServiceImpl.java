package com.eagle.system.base.infrastructure.service;

import com.eagle.system.common.exception.SystemErrorCode;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.service.RoleValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 角色分配领域服务实现
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class RoleValidationServiceImpl implements RoleValidationService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public void validateRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<Role> roles = roleRepository.findAllById(roleIds);

        // 校验所有角色存在
        if (roles.size() != roleIds.size()) {
            throw SystemErrorCode.ROLE_NOT_FOUND.toDomainException();
        }

        // 校验所有角色处于启用状态
        roles.stream()
                .filter(role -> !role.isActive())
                .findFirst()
                .ifPresent(role -> {
                    throw SystemErrorCode.ROLE_NOT_FOUND.toDomainException();
                });
    }
}

package com.eagle.system.system.domain.service;

import com.eagle.eagle.system.domain.model.Role;
import com.eagle.eagle.system.domain.model.User;
import com.eagle.eagle.system.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户领域服务
 * <p>
 * 处理跨聚合的业务逻辑，协调多个聚合根之间的交互
 * <p>
 * 使用场景：
 * <ul>
 *   <li>跨聚合的业务规则验证</li>
 *   <li>多个聚合根之间的协作</li>
 *   <li>复杂的业务计算</li>
 * </ul>
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final RoleRepository roleRepository;

    /**
     * 为用户分配角色（跨聚合操作）
     * <p>
     * 业务规则：
     * <ul>
     *   <li>角色必须存在</li>
     *   <li>角色必须是启用状态</li>
     *   <li>用户最多分配 10 个角色</li>
     * </ul>
     *
     * @param user   用户聚合根
     * @param roleId 角色 ID
     * @throws IllegalArgumentException 如果角色不存在
     * @throws IllegalStateException    如果角色已禁用或用户角色数量超限
     */
    public void assignRoleToUser(User user, Long roleId) {
        // 1. 检查角色是否存在
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        // 2. 检查角色状态
        if (!role.isActive()) {
            throw new IllegalStateException("角色已禁用，无法分配");
        }

        // 3. 委托给聚合根执行（包含业务规则校验）
        user.assignRole(roleId);
    }

    /**
     * 批量分配角色
     *
     * @param user    用户聚合根
     * @param roleIds 角色 ID 列表
     */
    public void assignRolesToUser(User user, Iterable<Long> roleIds) {
        for (Long roleId : roleIds) {
            assignRoleToUser(user, roleId);
        }
    }

    /**
     * 从用户移除角色
     *
     * @param user   用户聚合根
     * @param roleId 角色 ID
     */
    public void removeRoleFromUser(User user, Long roleId) {
        user.removeRole(roleId);
    }
}

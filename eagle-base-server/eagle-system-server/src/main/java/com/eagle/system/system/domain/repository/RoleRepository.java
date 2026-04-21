package com.eagle.system.system.domain.repository;

import com.eagle.system.domain.model.Role;
import com.eagle.system.domain.model.enums.RoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    /**
     * 通过角色标识查找角色
     *
     * @param roleCode 角色标识
     * @return 角色实体
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 检查角色标识是否已存在
     *
     * @param roleCode 角色标识
     * @return 是否存在
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 统计指定状态的角色数
     *
     * @param status 角色状态
     * @return 角色数量
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.status = :status")
    Long countByStatus(@Param("status") RoleStatus status);
}
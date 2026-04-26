package com.eagle.system.upms.domain.repository;

import com.eagle.system.upms.domain.model.RoleDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * 角色部门关联 Repository
 *
 * @author 孙士雄
 */
@Repository
public interface RoleDeptRepository extends JpaRepository<RoleDept, Long> {

    /**
     * 根据角色 ID 查询关联的部门 ID 集合。
     *
     * @param roleId 角色 ID
     * @return 部门 ID 集合
     */
    @Query("SELECT rd.deptId FROM RoleDept rd WHERE rd.roleId = :roleId")
    Set<Long> findDeptIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色 ID 集合查询关联的部门 ID 集合。
     *
     * @param roleIds 角色 ID 集合
     * @return 部门 ID 集合
     */
    @Query("SELECT rd.deptId FROM RoleDept rd WHERE rd.roleId IN :roleIds")
    Set<Long> findDeptIdsByRoleIdIn(@Param("roleIds") Set<Long> roleIds);
}

package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 角色与部门关联实体（支持 CUSTOM 数据权限）。
 *
 * @author 孙士雄
 */
@Getter
@Entity
@Table(name = "sys_role_dept", comment = "角色部门关联表",
        indexes = {
                @Index(name = "idx_role_id", columnList = "role_id"),
                @Index(name = "idx_dept_id", columnList = "dept_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_dept", columnNames = {"role_id", "dept_id"})
        })
@NoArgsConstructor
public class RoleDept extends BaseEntity {

    @Column(name = "role_id", nullable = false, comment = "角色 ID")
    private Long roleId;

    @Column(name = "dept_id", nullable = false, comment = "部门 ID")
    private Long deptId;

    public static RoleDept create(Long roleId, Long deptId) {
        RoleDept roleDept = new RoleDept();
        roleDept.roleId = roleId;
        roleDept.deptId = deptId;
        return roleDept;
    }
}

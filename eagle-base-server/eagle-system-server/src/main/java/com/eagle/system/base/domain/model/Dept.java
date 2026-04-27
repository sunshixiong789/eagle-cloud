package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.base.domain.model.enums.DeptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 部门聚合根（充血模型）
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "sys_dept", comment = "部门表", indexes = {
        @Index(name = "idx_parent_id_dept", columnList = "parent_id"),
        @Index(name = "idx_dept_path", columnList = "dept_path"),
        @Index(name = "idx_leader_id", columnList = "leader_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class Dept extends BaseAggregateRoot<Dept> {

    @Column(comment = "父级部门 ID")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    @Column(nullable = false, length = 100, comment = "部门名称")
    private String name;

    @Column(length = 500, comment = "部门层级路径，如：/1/2/3/")
    private String deptPath;

    @Column(comment = "部门层级")
    private Integer level;

    @Column(comment = "负责人 ID")
    private Long leaderId;

    @Pattern(regexp = "^\\+?[\\d\\s\\-()]{6,20}$", message = "手机号格式不正确")
    @Column(length = 20, comment = "联系电话")
    private String phone;

    @NotNull(message = "排序值不能为空")
    @Column(nullable = false, comment = "排序值")
    private Integer sortOrder;

    @NotNull(message = "部门状态不能为空")
    @Column(nullable = false, length = 20, comment = "部门状态")
    @Enumerated(EnumType.STRING)
    private DeptStatus status = DeptStatus.NORMAL;

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建部门（静态工厂方法）
     */
    public static Dept create(Long parentId, String name, Long leaderId, String phone, Integer sortOrder) {
        Dept dept = new Dept();
        dept.parentId = parentId;
        dept.name = name;
        dept.leaderId = leaderId;
        dept.phone = phone;
        dept.sortOrder = sortOrder;
        dept.status = DeptStatus.NORMAL;
        return dept;
    }

    /**
     * 更新部门信息
     */
    public void updateInfo(String name, Long leaderId, String phone, Integer sortOrder) {
        if (name != null) {
            this.name = name;
        }
        if (leaderId != null) {
            this.leaderId = leaderId;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * 更新负责人
     */
    public void updateLeader(Long leaderId) {
        this.leaderId = leaderId;
    }

    /**
     * 设置部门路径和层级
     */
    public void setPathAndLevel(String deptPath, Integer level) {
        this.deptPath = deptPath;
        this.level = level;
    }

    /**
     * 启用部门
     */
    public void enable() {
        this.status = DeptStatus.NORMAL;
    }

    /**
     * 禁用部门
     */
    public void disable() {
        this.status = DeptStatus.DISABLED;
    }
}
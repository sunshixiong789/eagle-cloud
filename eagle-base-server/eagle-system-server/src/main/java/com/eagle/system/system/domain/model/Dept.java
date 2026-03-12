package com.eagle.system.system.domain.model;

import com.eagle.eagle.common.base.BaseEntity;
import com.eagle.eagle.system.domain.model.enums.DeptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 部门
 *
 * @author sunshixiong
 */
@Getter
@Entity
@Table(name = "sys_dept", comment = "部门表", indexes = {
        @Index(name = "idx_parent_id_dept", columnList = "parent_id"),
        @Index(name = "idx_dept_path", columnList = "dept_path"),
        @Index(name = "idx_leader_id", columnList = "leader_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class Dept extends BaseEntity {

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

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Column(length = 20, comment = "联系电话")
    private String phone;

    @NotNull(message = "排序值不能为空")
    @Column(nullable = false, comment = "排序值")
    private Integer sortOrder;

    @NotNull(message = "部门状态不能为空")
    @Column(nullable = false, length = 20, comment = "部门状态")
    @Enumerated
    private DeptStatus status = DeptStatus.NORMAL;

}
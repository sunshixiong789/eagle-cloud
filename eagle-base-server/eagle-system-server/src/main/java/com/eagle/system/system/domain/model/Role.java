package com.eagle.system.system.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.domain.model.enums.DataScope;
import com.eagle.system.domain.model.enums.RoleStatus;
import com.eagle.system.domain.model.enums.RoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 角色聚合根（充血模型）
 *
 * @author sunshixiong
 */
@Getter
@Entity
@Table(name = "sys_role", comment = "系统角色表", indexes = {
        @Index(name = "idx_role_code", columnList = "role_code", unique = true),
        @Index(name = "idx_role_type", columnList = "role_type"),
        @Index(name = "idx_status_role", columnList = "status")
})
@NoArgsConstructor
public class Role extends BaseAggregateRoot<Role> {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    @Column(nullable = false, length = 64, comment = "角色名称")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 64, message = "角色标识长度不能超过64个字符")
    @Column(nullable = false, length = 64, unique = true, comment = "角色标识")
    private String roleCode;

    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    @Column(length = 255, comment = "角色描述")
    private String roleDesc;

    @NotNull(message = "角色类型不能为空")
    @Column(nullable = false, length = 20, comment = "角色类型")
    @Enumerated(EnumType.STRING)
    private RoleType roleType = RoleType.BUSINESS;

    @NotNull(message = "数据范围不能为空")
    @Column(nullable = false, length = 20, comment = "数据范围")
    @Enumerated(EnumType.STRING)
    private DataScope dataScope = DataScope.SELF;

    @NotNull(message = "排序值不能为空")
    @Column(nullable = false, comment = "排序值")
    private Integer sortOrder = 0;

    @NotNull(message = "角色状态不能为空")
    @Column(nullable = false, length = 20, comment = "角色状态")
    @Enumerated(EnumType.STRING)
    private RoleStatus status = RoleStatus.NORMAL;

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建业务角色（静态工厂方法）
     */
    public static Role create(String roleName, String roleCode, String roleDesc, Integer sortOrder) {
        Role role = new Role();
        role.roleName = roleName;
        role.roleCode = roleCode;
        role.roleDesc = roleDesc;
        role.sortOrder = sortOrder;
        role.roleType = RoleType.BUSINESS;
        role.dataScope = DataScope.SELF;
        role.status = RoleStatus.NORMAL;
        return role;
    }

    /**
     * 创建系统角色（静态工厂方法）
     * <p>
     * 系统角色由初始化器预置，不可通过 API 创建或删除。
     *
     * @param roleName  角色名称
     * @param roleCode  角色标识
     * @param roleDesc  角色描述
     * @param sortOrder 排序值
     * @param dataScope 数据范围
     * @return 新创建的系统角色
     */
    public static Role createSystemRole(String roleName, String roleCode, String roleDesc,
                                        Integer sortOrder, DataScope dataScope) {
        Role role = new Role();
        role.roleName = roleName;
        role.roleCode = roleCode;
        role.roleDesc = roleDesc;
        role.sortOrder = sortOrder;
        role.roleType = RoleType.SYSTEM;
        role.dataScope = dataScope;
        role.status = RoleStatus.NORMAL;
        return role;
    }

    /**
     * 更新角色信息
     */
    public void updateInfo(String roleName, String roleDesc, Integer sortOrder) {
        if (roleName != null) {
            this.roleName = roleName;
        }
        if (roleDesc != null) {
            this.roleDesc = roleDesc;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * 设置数据范围
     */
    public void setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
    }

    /**
     * 启用角色
     */
    public void enable() {
        this.status = RoleStatus.NORMAL;
    }

    /**
     * 禁用角色
     */
    public void disable() {
        this.status = RoleStatus.DISABLED;
    }

    /**
     * 判断角色是否启用
     */
    public boolean isActive() {
        return RoleStatus.NORMAL.equals(this.status);
    }
}

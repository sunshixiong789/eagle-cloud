package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseEntity;
import com.eagle.system.base.domain.model.enums.DataScope;
import com.eagle.system.base.domain.model.enums.RoleStatus;
import com.eagle.system.base.domain.model.enums.RoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色实体（充血模型）
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "sys_role", comment = "系统角色表", indexes = {
        @Index(name = "idx_role_code", columnList = "role_code", unique = true),
        @Index(name = "idx_role_type", columnList = "role_type"),
        @Index(name = "idx_status_role", columnList = "status")
})
public class Role extends BaseEntity {

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
    @Enumerated
    private RoleType roleType = RoleType.BUSINESS;

    @NotNull(message = "数据范围不能为空")
    @Column(nullable = false, length = 20, comment = "数据范围")
    @Enumerated
    private DataScope dataScope = DataScope.SELF;

    @NotNull(message = "排序值不能为空")
    @Column(nullable = false, comment = "排序值")
    private Integer sortOrder = 0;

    @NotNull(message = "角色状态不能为空")
    @Column(nullable = false, length = 20, comment = "角色状态")
    @Enumerated
    private RoleStatus status = RoleStatus.NORMAL;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_user_role",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    private List<Menu> menus = new ArrayList<>();

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建角色（静态工厂方法）
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
     * 分配菜单权限
     */
    public void assignMenus(List<Menu> menus) {
        this.menus.clear();
        if (menus != null) {
            this.menus.addAll(menus);
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

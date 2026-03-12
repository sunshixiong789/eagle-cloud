package com.eleganteer.system.system.domain.model;

import com.eleganteer.eleganteer.common.base.BaseEntity;
import com.eleganteer.eleganteer.system.domain.model.enums.DataScope;
import com.eleganteer.eleganteer.system.domain.model.enums.RoleStatus;
import com.eleganteer.eleganteer.system.domain.model.enums.RoleType;
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

import java.util.List;

/**
 * 角色
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
    private List<Menu> menus;

    // ==================== 业务方法 ====================

    /**
     * 判断角色是否启用
     *
     * @return true 表示启用
     */
    public boolean isActive() {
        return RoleStatus.NORMAL.equals(this.status);
    }

}

package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseEntity;
import com.eagle.system.base.domain.model.enums.MenuStatus;
import com.eagle.system.base.domain.model.enums.MenuType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 菜单实体（充血模型）
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "sys_menu", comment = "系统菜单表", indexes = {
        @Index(name = "idx_parent_id_menu", columnList = "parent_id"),
        @Index(name = "idx_permission", columnList = "permission"),
        @Index(name = "idx_menu_path", columnList = "menu_path")
})
public class Menu extends BaseEntity {

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过64个字符")
    @Column(nullable = false, length = 64, comment = "菜单名称")
    private String name;

    @Size(max = 64, message = "英文名称长度不能超过64个字符")
    @Column(length = 64, comment = "英文名称")
    private String enName;

    @Size(max = 128, message = "权限标识长度不能超过128个字符")
    @Column(length = 128, comment = "权限标识")
    private String permission;

    @NotNull(message = "父级菜单不能为空,为空时填0L")
    @Column(nullable = false, comment = "父级菜单ID")
    private Long parentId;

    @Column(length = 500, comment = "菜单层级路径，如：/1/2/3/")
    private String menuPath;

    @Column(comment = "菜单层级")
    private Integer level;

    @Size(max = 128, message = "图标长度不能超过128个字符")
    @Column(length = 128, comment = "菜单图标")
    private String icon;

    @Size(max = 500, message = "路径长度不能超过500个字符")
    @Column(length = 500, comment = "路由路径")
    private String path;

    @Size(max = 255, message = "组件路径长度不能超过255个字符")
    @Column(comment = "组件路径")
    private String component;

    @Column(nullable = false, comment = "是否可见")
    private Boolean visible = true;

    @Column(comment = "排序值")
    private Integer sortOrder;

    @NotNull(message = "菜单类型不能为空")
    @Column(nullable = false, length = 20, comment = "菜单类型")
    @Enumerated
    private MenuType menuType;

    @Column(nullable = false, comment = "是否缓存")
    private Boolean keepAlive = false;

    @Column(nullable = false, comment = "是否嵌入")
    private Boolean embedded = false;

    @Column(nullable = false, comment = "是否为外链")
    private Boolean isFrame = false;

    @NotNull(message = "菜单状态不能为空")
    @Column(nullable = false, length = 20, comment = "菜单状态")
    @Enumerated
    private MenuStatus status = MenuStatus.ACTIVE;

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建菜单（静态工厂方法）
     */
    public static Menu create(String name, String enName, String permission, Long parentId,
                              String icon, String path, String component, Boolean visible,
                              Integer sortOrder, MenuType menuType) {
        Menu menu = new Menu();
        menu.name = name;
        menu.enName = enName;
        menu.permission = permission;
        menu.parentId = parentId;
        menu.icon = icon;
        menu.path = path;
        menu.component = component;
        menu.visible = visible != null ? visible : true;
        menu.sortOrder = sortOrder;
        menu.menuType = menuType;
        menu.keepAlive = false;
        menu.embedded = false;
        menu.isFrame = false;
        menu.status = MenuStatus.ACTIVE;
        return menu;
    }

    /**
     * 更新菜单信息
     */
    public void updateInfo(String name, String enName, String permission, String icon,
                          String path, String component, Boolean visible, Integer sortOrder) {
        if (name != null) {
            this.name = name;
        }
        if (enName != null) {
            this.enName = enName;
        }
        if (permission != null) {
            this.permission = permission;
        }
        if (icon != null) {
            this.icon = icon;
        }
        if (path != null) {
            this.path = path;
        }
        if (component != null) {
            this.component = component;
        }
        if (visible != null) {
            this.visible = visible;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * 设置菜单路径和层级
     */
    public void setPathAndLevel(String menuPath, Integer level) {
        this.menuPath = menuPath;
        this.level = level;
    }

    /**
     * 显示菜单
     */
    public void show() {
        this.visible = true;
    }

    /**
     * 隐藏菜单
     */
    public void hide() {
        this.visible = false;
    }

    /**
     * 启用菜单
     */
    public void enable() {
        this.status = MenuStatus.ACTIVE;
    }

    /**
     * 禁用菜单
     */
    public void disable() {
        this.status = MenuStatus.INACTIVE;
    }

    /**
     * 设置缓存
     */
    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * 判断是否为目录
     */
    public boolean isDirectory() {
        return MenuType.DIRECTORY.equals(this.menuType);
    }

    /**
     * 判断是否为菜单
     */
    public boolean isMenu() {
        return MenuType.MENU.equals(this.menuType);
    }

    /**
     * 判断是否为按钮
     */
    public boolean isButton() {
        return MenuType.BUTTON.equals(this.menuType);
    }
}

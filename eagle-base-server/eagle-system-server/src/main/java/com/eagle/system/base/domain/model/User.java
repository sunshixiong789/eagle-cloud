package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.base.domain.event.UserCreatedEvent;
import com.eagle.system.base.domain.event.UserLockedEvent;
import com.eagle.system.base.domain.event.UserPasswordChangedEvent;
import com.eagle.system.base.domain.model.valueobject.Address;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户聚合根（充血模型）
 * <p>
 * 聚合边界：
 * <ul>
 *   <li>聚合内部：UserProfile（值对象）、Address（值对象）</li>
 *   <li>聚合外部引用：DeptId、RoleIds、PostIds（只保存 ID）</li>
 * </ul>
 * <p>
 * 业务不变性：
 * <ul>
 *   <li>用户名全局唯一</li>
 *   <li>用户名和邮箱至少填写一个</li>
 *   <li>用户最多分配 10 个角色</li>
 *   <li>锁定状态不能重复设置</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "sys_user", comment = "系统用户表", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true),
        @Index(name = "idx_phone", columnList = "phone"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_dept_id", columnList = "dept_id")
})
public class User extends BaseAggregateRoot<User> {

    // ==================== 聚合内部（值对象）====================

    @Embedded
    private UserProfile profile;

    @Embedded
    private Address address;

    // ==================== 聚合根属性 ====================

    @Column(nullable = false, unique = true, length = 64, comment = "用户名")
    private String username;

    @RestResource(exported = false)
    @Column(nullable = false, length = 128, comment = "密码")
    private String password;

    @Column(length = 20, comment = "手机号")
    private String phone;

    @Column(length = 100, comment = "邮箱")
    private String email;

    @Column(length = 10, comment = "锁定标识")
    private Boolean lockFlag = false;

    // ==================== 聚合外部引用（只保存 ID）====================

    @Column(name = "dept_id", comment = "部门 ID")
    private Long deptId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_id")
    private Set<Long> roleIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "sys_user_post",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "post_id")
    private Set<Long> postIds = new HashSet<>();

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建新用户（静态工厂方法）
     */
    public static User create(String username, String password, String phone, String email, UserProfile profile) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("手机号和邮箱至少填写一个");
        }

        User user = new User();
        user.username = username;
        user.password = password;
        user.phone = phone;
        user.email = email;
        user.profile = profile;
        user.lockFlag = false;
        user.registerEvent(new UserCreatedEvent(null, username, phone, email));
        return user;
    }

    /**
     * 修改密码
     */
    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        this.password = newPassword;
        this.registerEvent(new UserPasswordChangedEvent(this.getId(), this.username));
    }

    /**
     * 锁定用户
     */
    public void lock(String reason) {
        if (Boolean.TRUE.equals(this.lockFlag)) {
            throw new IllegalStateException("用户已被锁定");
        }
        this.lockFlag = true;
        this.registerEvent(new UserLockedEvent(this.getId(), this.username, reason));
    }

    /**
     * 解锁用户
     */
    public void unlock() {
        if (Boolean.FALSE.equals(this.lockFlag)) {
            throw new IllegalStateException("用户未被锁定");
        }
        this.lockFlag = false;
    }

    /**
     * 更新用户资料
     */
    public void updateProfile(UserProfile newProfile) {
        this.profile = newProfile;
    }

    /**
     * 更新联系方式
     */
    public void updateContact(String phone, String email) {
        if (phone != null) {
            this.phone = phone;
        }
        if (email != null) {
            this.email = email;
        }
    }

    /**
     * 分配角色
     */
    public void assignRoles(Set<Long> roleIds) {
        if (roleIds != null && roleIds.size() > 10) {
            throw new IllegalArgumentException("用户最多分配 10 个角色");
        }
        this.roleIds = roleIds != null ? new HashSet<>(roleIds) : new HashSet<>();
    }

    /**
     * 分配部门
     */
    public void assignDept(Long deptId) {
        this.deptId = deptId;
    }

    /**
     * 分配岗位
     */
    public void assignPosts(Set<Long> postIds) {
        this.postIds = postIds != null ? new HashSet<>(postIds) : new HashSet<>();
    }
}
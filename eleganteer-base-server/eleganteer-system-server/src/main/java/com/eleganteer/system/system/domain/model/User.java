package com.eleganteer.system.system.domain.model;

import com.eleganteer.eleganteer.common.base.BaseEventEntity;
import com.eleganteer.eleganteer.system.domain.event.UserCreatedEvent;
import com.eleganteer.eleganteer.system.domain.event.UserPasswordChangedEvent;
import com.eleganteer.eleganteer.system.domain.model.valueobject.Address;
import com.eleganteer.eleganteer.system.domain.model.valueobject.UserProfile;
import com.eleganteer.eleganteer.system.domain.service.PasswordEncryptor;
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
@Table(name = "sys_user", comment = "系统用户表", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true),
        @Index(name = "idx_phone", columnList = "phone"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_dept_id", columnList = "dept_id")
})
@NoArgsConstructor
public class User extends BaseEventEntity {

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

    // ==================== 业务行为方法 ====================

    /**
     * 初始化新用户（创建时调用）
     * <p>
     * 业务规则：
     * - 用户名和邮箱至少填写一个
     * - 设置初始状态为未锁定
     * - 初始化用户资料
     * - 发布用户创建事件
     *
     * @param encryptor 密码加密器
     * @param plainPassword 明文密码
     */
    public void initializeAsNewUser(PasswordEncryptor encryptor, String plainPassword) {
        // 前置条件检查
        validateContactInfo();
        validateUsername();

        // 设置业务状态
        this.password = encryptor.encrypt(plainPassword);
        this.lockFlag = false;

        // 初始化嵌入对象
        if (this.profile == null) {
            this.profile = new UserProfile();
        }

        // 初始化集合
        if (this.roleIds == null) {
            this.roleIds = new HashSet<>();
        }
        if (this.postIds == null) {
            this.postIds = new HashSet<>();
        }

        // 发布领域事件
        registerEvent(new UserCreatedEvent(this.getId(), this.username, this.phone, this.email));
    }

    /**
     * 锁定用户
     *
     * @throws IllegalStateException 如果用户已被锁定
     */
    public void lock() {
        if (Boolean.TRUE.equals(this.lockFlag)) {
            throw new IllegalStateException("用户已经被锁定");
        }
        this.lockFlag = true;
    }

    /**
     * 解锁用户
     *
     * @throws IllegalStateException 如果用户未被锁定
     */
    public void unlock() {
        if (Boolean.FALSE.equals(this.lockFlag)) {
            throw new IllegalStateException("用户未被锁定，无需解锁");
        }
        this.lockFlag = false;
    }

    /**
     * 修改密码
     * <p>
     * 发布 UserPasswordChangedEvent 领域事件
     *
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @param encryptor   密码加密器
     * @throws IllegalArgumentException 如果旧密码不正确
     */
    public void changePassword(String oldPassword, String newPassword, PasswordEncryptor encryptor) {
        if (!encryptor.matches(oldPassword, this.password)) {
            throw new IllegalArgumentException("原密码不正确");
        }
        this.password = encryptor.encrypt(newPassword);

        // 发布密码修改事件
        registerEvent(new UserPasswordChangedEvent(this.getId(), this.username));
    }

    /**
     * 更新用户资料
     * <p>
     * 通过聚合根方法修改内部值对象，保证一致性
     *
     * @param name     真实姓名
     * @param nickname 昵称
     * @param avatar   头像 URL
     */
    public void updateProfile(String name, String nickname, String avatar) {
        if (this.profile == null) {
            this.profile = new UserProfile();
        }
        // 值对象不可变，创建新对象替换
        this.profile = this.profile.update(name, nickname, avatar);
    }

    /**
     * 分配到部门
     *
     * @param deptId 部门 ID
     */
    public void assignToDept(Long deptId) {
        if (deptId == null) {
            throw new IllegalArgumentException("部门 ID 不能为空");
        }
        this.deptId = deptId;
    }

    /**
     * 分配角色
     * <p>
     * 业务规则：用户最多分配 10 个角色
     *
     * @param roleId 角色 ID
     */
    public void assignRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色 ID 不能为空");
        }
        if (this.roleIds.size() >= 10) {
            throw new IllegalStateException("用户角色数量不能超过 10 个");
        }
        this.roleIds.add(roleId);
    }

    /**
     * 移除角色
     *
     * @param roleId 角色 ID
     */
    public void removeRole(Long roleId) {
        this.roleIds.remove(roleId);
    }

    /**
     * 分配岗位
     *
     * @param postId 岗位 ID
     */
    public void assignPost(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("岗位 ID 不能为空");
        }
        this.postIds.add(postId);
    }

    /**
     * 移除岗位
     *
     * @param postId 岗位 ID
     */
    public void removePost(Long postId) {
        this.postIds.remove(postId);
    }

    // ==================== 私有辅助方法（业务规则校验）====================

    /**
     * 校验联系方式
     * <p>
     * 业务规则：用户名和邮箱至少填写一个
     */
    private void validateContactInfo() {
        if ((this.username == null || this.username.isBlank()) &&
            (this.email == null || this.email.isBlank())) {
            throw new IllegalArgumentException("用户名和邮箱至少填写一个");
        }
    }

    /**
     * 校验用户名格式
     */
    private void validateUsername() {
        if (this.username != null && !this.username.isBlank()) {
            if (this.username.length() < 2 || this.username.length() > 64) {
                throw new IllegalArgumentException("用户名长度必须在 2-64 个字符之间");
            }
            if (!this.username.matches("^[a-zA-Z0-9_-]{2,64}$")) {
                throw new IllegalArgumentException("用户名只能包含字母、数字、下划线和中划线");
            }
        }
    }

    // ==================== Setter（仅供 JPA 和 MapStruct 使用）====================

    /**
     * 设置用户名
     * <p>
     * 注意：此方法仅供 JPA 和 MapStruct 使用，业务代码不应直接调用
     */
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public void setLockFlag(Boolean lockFlag) {
        this.lockFlag = lockFlag;
    }

}
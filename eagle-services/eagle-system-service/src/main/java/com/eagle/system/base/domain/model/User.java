package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.common.exception.DomainException;
import com.eagle.system.common.exception.UserErrorCode;
import com.eagle.system.base.domain.event.UserCreatedEvent;
import com.eagle.system.base.domain.event.UserUpdatedEvent;
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

import java.util.HashSet;
import java.util.Set;

/**
 * 用户聚合根（充血模型）
 * <p>
 * 聚合边界：
 * <ul>
 *   <li>聚合内部：UserProfile（值对象）、Address（值对象）</li>
 *   <li>聚合外部引用：AccountId、DeptId、RoleIds、PostIds（只保存 ID）</li>
 * </ul>
 * <p>
 * 业务不变性：
 * <ul>
 *   <li>accountId 必填（关联认证账号）</li>
 *   <li>用户最多分配 10 个角色</li>
 * </ul>
 * <p>
 * 认证相关字段（password、phone、locked、wechatBinding）已迁移至 auth 域的 Account 聚合。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "sys_user", comment = "系统用户表", indexes = {
        @Index(name = "idx_account_id", columnList = "account_id", unique = true),
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_user_dept_id", columnList = "dept_id")
})
public class User extends BaseAggregateRoot<User> {

    // ==================== 聚合内部（值对象）====================

    @Embedded
    private UserProfile profile;

    @Embedded
    private Address address;

    // ==================== 聚合根属性 ====================

    @Column(name = "account_id", unique = true, nullable = false, comment = "关联的认证账号 ID")
    private Long accountId;

    @Column(nullable = false, length = 64, comment = "用户名（冗余，来源于 Account）")
    private String username;

    @Column(length = 100, comment = "邮箱")
    private String email;

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
     * <p>
     * 业务规则：
     * <ul>
     *   <li>accountId 必填（关联认证账号）</li>
     *   <li>新用户默认无部门、无角色</li>
     * </ul>
     *
     * @param accountId 关联的 Account ID（必填）
     * @param username  用户名（冗余，来源于 Account）
     * @param email     邮箱（可选）
     * @param profile   用户资料（可选）
     * @return 新创建的用户实例
     * @throws DomainException 当 accountId 为空时
     */
    public static User create(Long accountId, String username, String email, UserProfile profile) {
        if (accountId == null) {
            throw UserErrorCode.USERNAME_REQUIRED.toDomainException();
        }
        User user = new User();
        user.accountId = accountId;
        user.username = username;
        user.email = email;
        user.profile = profile;
        user.registerEvent(new UserCreatedEvent(null, username, null, email));
        return user;
    }

    /**
     * 从 AccountCreatedEvent 创建用户（社交/短信登录自动注册）
     *
     * @param accountId 关联的 Account ID
     * @param username  用户名（冗余）
     * @param phone     手机号（仅用于事件，User 不存储）
     * @param profile   用户资料（可选）
     * @return 新创建的用户
     */
    public static User createForAccount(Long accountId, String username,
                                         String phone, UserProfile profile) {
        User user = new User();
        user.accountId = accountId;
        user.username = username;
        user.email = null;
        user.profile = profile;
        user.registerEvent(new UserCreatedEvent(null, username, phone, null));
        return user;
    }

    /**
     * 更新用户资料
     */
    public void updateProfile(UserProfile newProfile) {
        this.profile = newProfile;
        this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
    }

    /**
     * 更新联系方式
     *
     * @param email 邮箱（可选）
     */
    public void updateContact(String email) {
        if (email != null) {
            this.email = email;
        }
        this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
    }

    /**
     * 分配角色
     * <p>
     * 业务规则：
     * <ul>
     *   <li>用户最多分配 10 个角色（防止权限过度膨胀）</li>
     *   <li>传入 null 表示清空所有角色</li>
     * </ul>
     *
     * @param roleIds 角色 ID 集合（null 表示清空角色）
     * @throws DomainException 当角色数量超过 10 个时
     */
    public void assignRoles(Set<Long> roleIds) {
        // 校验业务不变性：用户最多分配 10 个角色
        if (roleIds != null && roleIds.size() > 10) {
            throw UserErrorCode.MAX_ROLES_EXCEEDED.toDomainException();
        }
        // 在原 JPA 托管集合上操作，避免替换 PersistentSet 导致变更丢失
        this.roleIds.clear();
        if (roleIds != null) {
            this.roleIds.addAll(roleIds);
        }
        this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
    }

    /**
     * 分配部门
     */
    public void assignDept(Long deptId) {
        this.deptId = deptId;
        this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
    }

    /**
     * 分配岗位
     */
    public void assignPosts(Set<Long> postIds) {
        this.postIds.clear();
        if (postIds != null) {
            this.postIds.addAll(postIds);
        }
        this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
    }
}
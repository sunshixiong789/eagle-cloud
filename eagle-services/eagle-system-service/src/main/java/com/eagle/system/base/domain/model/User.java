package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.common.exception.DomainException;
import com.eagle.system.base.domain.event.UserCreatedEvent;
import com.eagle.system.base.domain.event.UserUpdatedEvent;
import com.eagle.system.base.domain.model.valueobject.Address;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户聚合根（充血模型）
 * <p>
 * 聚合边界：
 * <ul>
 *   <li>聚合内部：UserProfile（值对象）、Address（值对象）</li>
 *   <li>聚合外部引用：AccountId（只保存 ID）</li>
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
        @Index(name = "idx_email", columnList = "email")
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

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建新用户（静态工厂方法）
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
     */
    public void updateContact(String email) {
        if (email != null) {
            this.email = email;
        }
        this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
    }
}

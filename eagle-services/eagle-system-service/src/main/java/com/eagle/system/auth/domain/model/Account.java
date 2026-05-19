package com.eagle.system.auth.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.event.AccountDeletedEvent;
import com.eagle.system.auth.domain.event.AccountFrozenEvent;
import com.eagle.system.auth.domain.event.AccountRegisteredEvent;
import com.eagle.system.auth.domain.event.AccountUnfrozenEvent;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.model.enums.FreezeReason;
import com.eagle.system.auth.domain.model.valueobject.AccountFreeze;
import com.eagle.system.auth.domain.model.valueobject.ProfileHints;
import com.eagle.system.auth.domain.model.valueobject.WechatBinding;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账号聚合根
 * <p>
 * 管理用户的认证凭据（用户名、密码、微信绑定）和账号状态（冻结）。
 * 与 system 域的 User 通过 accountId 关联，Account 负责"你是谁"，
 * User 负责"你的组织信息和权限"。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "auth_account", indexes = {
        @Index(name = "idx_account_username", columnList = "username", unique = true),
        @Index(name = "idx_account_phone", columnList = "phone"),
        @Index(name = "idx_account_openid", columnList = "openid"),
        @Index(name = "idx_account_unionid", columnList = "unionid"),
        @Index(name = "idx_account_web_openid", columnList = "web_openid"),
        @Index(name = "idx_account_mp_openid", columnList = "mp_openid")
})
public class Account extends BaseAggregateRoot<Account> {

    /**
     * 用户名（唯一）
     */
    @Column(nullable = false, length = 64, unique = true, comment = "用户名")
    private String username;

    /**
     * 密码（BCrypt 加密）
     */
    @Column(nullable = false, length = 128, comment = "密码（BCrypt）")
    private String password;

    /**
     * 手机号
     */
    @Column(length = 20, comment = "手机号")
    private String phone;

    /**
     * 账号状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, comment = "账号状态")
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * 冻结信息（status=FROZEN 时非 null）
     */
    @Embedded
    private AccountFreeze freeze;

    /**
     * 微信绑定信息
     */
    @Embedded
    private WechatBinding wechatBinding;

    /**
     * 注册时的 profile 提示信息（瞬态，不持久化）
     * <p>
     * 工厂方法中设置，{@code @PostPersist} 回调中用于构建
     * {@link AccountRegisteredEvent}，事件发布后自动清除。
     */
    @Transient
    private ProfileHints profileHints;

    // ==================== 工厂方法 ====================

    /**
     * 通过用户名和密码创建账号（管理员/表单注册）
     *
     * @param username     用户名
     * @param password     加密后的密码
     * @param phone        手机号（可选）
     * @param profileHints 用户画像提示（传递给 system 域创建 User）
     * @return 新建的 Account 实例
     */
    public static Account create(String username, String password, String phone,
                                 ProfileHints profileHints) {
        if (username == null || username.isBlank()) {
            throw AuthErrorCode.ACCOUNT_USERNAME_REQUIRED.toDomainException();
        }
        if (password == null || password.isBlank()) {
            throw AuthErrorCode.PASSWORD_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = username;
        account.password = password;
        account.phone = phone;
        account.status = AccountStatus.ACTIVE;
        account.profileHints = profileHints;
        return account;
    }

    /**
     * 通过微信小程序 openid 创建账号
     *
     * @param openid  微信小程序 openid
     * @param unionid 微信 unionid（可选）
     * @return 新建的 Account 实例
     */
    public static Account createFromWechat(String openid, String unionid) {
        if (openid == null || openid.isBlank()) {
            throw AuthErrorCode.OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wx_" + openid.substring(0, Math.min(16, openid.length()));
        account.password = "";
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.create(openid, unionid);
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    /**
     * 通过手机号创建账号（短信验证码登录）
     *
     * @param phone 手机号
     * @return 新建的 Account 实例
     */
    public static Account createFromPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.PHONE_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = phone;
        account.password = "";
        account.phone = phone;
        account.status = AccountStatus.ACTIVE;
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    /**
     * 通过微信网页（PC 扫码）登录创建账号
     *
     * @param webOpenid 微信网页 openid
     * @param unionid   微信 unionid（可选）
     * @param nickname  微信昵称（可选）
     * @param avatar    微信头像 URL（可选）
     * @return 新建的 Account 实例
     */
    public static Account createFromWechatWeb(String webOpenid, String unionid,
                                              String nickname, String avatar) {
        if (webOpenid == null || webOpenid.isBlank()) {
            throw AuthErrorCode.WEB_OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wxweb_"
                + webOpenid.substring(0, Math.min(16, webOpenid.length()));
        account.password = "";
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.createForWeb(webOpenid, unionid);
        account.profileHints = ProfileHints.ofWechat(nickname, avatar);
        return account;
    }

    /**
     * 通过微信公众号 H5 登录创建账号
     *
     * @param mpOpenid 微信公众号 openid
     * @param unionid  微信 unionid（可选）
     * @param nickname 微信昵称（可选）
     * @param avatar   微信头像 URL（可选）
     * @return 新建的 Account 实例
     */
    public static Account createFromWechatH5(String mpOpenid, String unionid,
                                             String nickname, String avatar) {
        if (mpOpenid == null || mpOpenid.isBlank()) {
            throw AuthErrorCode.MP_OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wxmp_"
                + mpOpenid.substring(0, Math.min(16, mpOpenid.length()));
        account.password = "";
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.createForH5(mpOpenid, unionid);
        account.profileHints = ProfileHints.ofWechat(nickname, avatar);
        return account;
    }

    // ==================== 事件发布 ====================

    /**
     * JPA 持久化回调：首次 INSERT 后自动注册 AccountRegisteredEvent
     * <p>
     * 此时 ID 已由数据库分配（{@code GenerationType.IDENTITY}），
     * 事件携带完整的 accountId。Spring Data 在 {@code save()} 返回前
     * 读取已注册的领域事件并发布，因此只需一次 save 即可完成创建+事件发布。
     */
    @PostPersist
    private void onPostPersist() {
        if (profileHints != null) {
            registerEvent(new AccountRegisteredEvent(
                    getId(), username, phone,
                    profileHints.nickname(), profileHints.avatar(),
                    profileHints.email()
            ));
            profileHints = null;
        }
    }

    /**
     * 发布账号删除事件（跨域事件，system 域级联删除 User）
     */
    public void publishDeletedEvent() {
        registerEvent(new AccountDeletedEvent(getId()));
    }

    // ==================== 凭据管理 ====================

    /**
     * 修改密码
     *
     * @param newPassword 新密码（已加密）
     */
    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw AuthErrorCode.NEW_PASSWORD_REQUIRED.toDomainException();
        }
        this.password = newPassword;
    }

    /**
     * 绑定手机号（微信登录后补充手机号场景）
     *
     * @param phone 手机号
     */
    public void bindPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.PHONE_REQUIRED.toDomainException();
        }
        if (this.phone != null && !this.phone.isBlank()) {
            throw AuthErrorCode.ACCOUNT_PHONE_ALREADY_SET.toDomainException();
        }
        this.phone = phone;
    }

    // ==================== 冻结 / 解冻 ====================

    /**
     * 管理员冻结账号
     *
     * @param operatorId   操作人 ID
     * @param operatorName 操作人姓名
     * @param reason       冻结原因
     * @param freezeUntil  到期时间（null 表示永久冻结）
     * @param remark       备注
     */
    public void freezeByAdmin(Long operatorId, String operatorName,
                              FreezeReason reason, LocalDateTime freezeUntil, String remark) {
        if (this.status == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        if (freezeUntil != null && !freezeUntil.isAfter(LocalDateTime.now())) {
            throw AuthErrorCode.ACCOUNT_FREEZE_UNTIL_INVALID.toDomainException();
        }
        this.status = AccountStatus.FROZEN;
        this.freeze = new AccountFreeze(
                reason, freezeUntil, operatorId, operatorName, remark, LocalDateTime.now());
        registerEvent(new AccountFrozenEvent(getId(), username, reason, freezeUntil, operatorId));
    }

    /**
     * 管理员解冻账号
     *
     * @param operatorId   操作人 ID
     * @param operatorName 操作人姓名
     */
    public void unfreeze(Long operatorId, String operatorName) {
        if (this.status != AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_NOT_FROZEN.toDomainException();
        }
        this.status = AccountStatus.ACTIVE;
        this.freeze = null;
        registerEvent(new AccountUnfrozenEvent(
                getId(), username, AccountUnfrozenEvent.Source.ADMIN, operatorId));
    }

    /**
     * 尝试自动解冻（到期自动解冻）
     *
     * @param now 当前时间
     * @return true 表示已解冻，false 表示未到期或非冻结状态
     */
    public boolean tryAutoUnfreezeIfExpired(LocalDateTime now) {
        if (this.status == AccountStatus.FROZEN
                && this.freeze != null
                && this.freeze.isExpired(now)) {
            this.status = AccountStatus.ACTIVE;
            this.freeze = null;
            registerEvent(new AccountUnfrozenEvent(
                    getId(), username, AccountUnfrozenEvent.Source.AUTO, null));
            return true;
        }
        return false;
    }

    // ==================== 旧 lock/unlock @Deprecated 委托 ====================

    /**
     * @deprecated use {@link #freezeByAdmin}
     */
    @Deprecated
    public void lock() {
        freezeByAdmin(null, "system-legacy",
                FreezeReason.OTHER, null, "legacy lock API");
    }

    /**
     * @deprecated use {@link #unfreeze}
     */
    @Deprecated
    public void unlock() {
        unfreeze(null, "system-legacy");
    }

    // ==================== 微信绑定 ====================

    /**
     * 绑定微信小程序
     *
     * @param openid  微信小程序 openid
     * @param unionid 微信 unionid（可选）
     */
    public void bindWechat(String openid, String unionid) {
        if (openid == null || openid.isBlank()) {
            throw AuthErrorCode.OPENID_REQUIRED.toDomainException();
        }
        this.wechatBinding = WechatBinding.create(openid, unionid);
    }

    /**
     * 绑定微信网页（PC 扫码）
     *
     * @param webOpenid 微信网页 openid
     * @param unionid   微信 unionid（可选）
     */
    public void bindWechatWeb(String webOpenid, String unionid) {
        if (webOpenid == null || webOpenid.isBlank()) {
            throw AuthErrorCode.WEB_OPENID_REQUIRED.toDomainException();
        }
        if (this.wechatBinding == null) {
            this.wechatBinding = WechatBinding.createForWeb(webOpenid, unionid);
        } else {
            this.wechatBinding = this.wechatBinding.withWebOpenid(webOpenid);
            if (unionid != null) {
                this.wechatBinding = this.wechatBinding.withUnionid(unionid);
            }
        }
    }

    /**
     * 绑定微信公众号 H5
     *
     * @param mpOpenid 微信公众号 openid
     * @param unionid  微信 unionid（可选）
     */
    public void bindWechatH5(String mpOpenid, String unionid) {
        if (mpOpenid == null || mpOpenid.isBlank()) {
            throw AuthErrorCode.MP_OPENID_REQUIRED.toDomainException();
        }
        if (this.wechatBinding == null) {
            this.wechatBinding = WechatBinding.createForH5(mpOpenid, unionid);
        } else {
            this.wechatBinding = this.wechatBinding.withMpOpenid(mpOpenid);
            if (unionid != null) {
                this.wechatBinding = this.wechatBinding.withUnionid(unionid);
            }
        }
    }
}

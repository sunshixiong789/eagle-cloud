package com.eagle.auth.core.domain.model;

import com.eagle.datajpa.base.BaseAggregateRoot;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.event.AccountDeletedEvent;
import com.eagle.auth.core.domain.event.AccountFrozenEvent;
import com.eagle.auth.core.domain.event.AccountPhoneChangedEvent;
import com.eagle.auth.core.domain.event.AccountRegisteredEvent;
import com.eagle.auth.core.domain.event.AccountUnfrozenEvent;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.model.enums.FreezeReason;
import com.eagle.auth.core.domain.model.valueobject.AccountFreeze;
import com.eagle.auth.core.domain.model.valueobject.AppleBinding;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import com.eagle.auth.core.domain.model.valueobject.TaobaoBinding;
import com.eagle.auth.core.domain.model.valueobject.WechatBinding;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 账号聚合根。
 *
 * <p>管理用户的认证凭据（用户名、密码、微信绑定）和账号状态（冻结）。
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
        @Index(name = "idx_account_phone", columnList = "phone", unique = true),
        // 第三方身份 → 账号一对一（手机号为主账号体系），unique 兜底并发；
        // MySQL 唯一索引允许多行 NULL，不影响未绑定账号。
        // 注意：ddl-auto=update 不会把既有普通索引升级为 unique，存量库需手动 DROP 旧索引。
        @Index(name = "idx_account_openid", columnList = "openid", unique = true),
        @Index(name = "idx_account_unionid", columnList = "unionid", unique = true),
        @Index(name = "idx_account_web_openid", columnList = "web_openid", unique = true),
        @Index(name = "idx_account_mp_openid", columnList = "mp_openid", unique = true),
        @Index(name = "idx_account_taobao_open_uid", columnList = "taobao_open_uid", unique = true),
        @Index(name = "idx_account_apple_subject", columnList = "apple_subject", unique = true)
})
public class Account extends BaseAggregateRoot<Account> {

    /**
     * 未启用密码登录的占位 BCrypt 哈希（格式合法但原文未知，{@code BCryptPasswordEncoder.matches}
     * 对任意输入都返回 false，且不会打印 "Encoded password does not look like BCrypt" 警告）。
     *
     * <p>微信 / 短信 / 一键登录创建的账号使用此占位，DAO 表单密码登录路径自然失败；
     * 自定义 grant 路径不走密码比对，不受影响。
     */
    public static final String DISABLED_PASSWORD =
            "$2a$10$0000000000000000000000.0000000000000000000000000000000000";

    /**
     * 用户名（唯一）
     */
    @Column(nullable = false, length = 64, unique = true, comment = "用户名")
    private String username;

    /**
     * 密码（BCrypt 加密；或 {@link #DISABLED_PASSWORD} 占位）
     */
    @Column(nullable = false, length = 128, comment = "密码（BCrypt 或 {disabled} 占位）")
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
     * 淘宝绑定信息
     */
    @Embedded
    private TaobaoBinding taobaoBinding;

    /** Apple Sign In 绑定信息。 */
    @Embedded
    private AppleBinding appleBinding;

    /**
     * 注册时的 profile 提示信息（瞬态，不持久化）。
     *
     * <p>工厂方法中设置，{@code @PostPersist} 回调中用于构建
     * {@link AccountRegisteredEvent}，事件发布后自动清除。
     */
    @Transient
    private ProfileHints profileHints;

    // ==================== 工厂方法 ====================

    /**
     * 通过用户名和密码创建账号（管理员 / 表单注册）。
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
     * 通过微信小程序 openid 创建账号。
     *
     * <p>username 用 openid 的 SHA-256 哈希前 16 字符（小写 hex），避免单纯截前 16 字符导致碰撞。
     * 密码占位为 {@link #DISABLED_PASSWORD}，不可通过表单密码登录。
     */
    public static Account createFromWechat(String openid, String unionid) {
        if (openid == null || openid.isBlank()) {
            throw AuthErrorCode.OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wx_" + shortHash(openid);
        account.password = DISABLED_PASSWORD;
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.create(openid, unionid);
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    /**
     * 通过手机号创建账号（短信验证码登录）。
     *
     * <p>username 使用手机号的稳定哈希，避免换绑后旧手机号仍被 username 唯一索引占用。
     * 手机登录的账号识别以 {@code phone} 字段为准；密码占位 {@link #DISABLED_PASSWORD}。
     */
    public static Account createFromPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.PHONE_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "phone_" + shortHash(phone);
        account.password = DISABLED_PASSWORD;
        account.phone = phone;
        account.status = AccountStatus.ACTIVE;
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    /**
     * 通过淘宝 openUid 创建账号（淘宝一键登录，新用户直登，无需手机号）。
     *
     * <p>username 用 openUid 的 SHA-256 哈希前 16 字符（小写 hex），与 {@link #createFromWechat} 同规则。
     * 密码占位为 {@link #DISABLED_PASSWORD}，不可通过表单密码登录。
     */
    public static Account createFromTaobao(String openUid) {
        if (openUid == null || openUid.isBlank()) {
            throw AuthErrorCode.TAOBAO_AUTH_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "tb_" + shortHash(openUid);
        account.password = DISABLED_PASSWORD;
        account.status = AccountStatus.ACTIVE;
        account.taobaoBinding = TaobaoBinding.create(openUid);
        account.profileHints = ProfileHints.EMPTY;
        return account;
    }

    /**
     * 通过服务端验签后的 Apple subject 创建账号。
     */
    public static Account createFromApple(
            String subject, String email, String fullName,
            String encryptedRefreshToken) {
        if (subject == null || subject.isBlank()) {
            throw AuthErrorCode.APPLE_SUBJECT_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "apple_" + shortHash(subject);
        account.password = DISABLED_PASSWORD;
        account.status = AccountStatus.ACTIVE;
        account.appleBinding = AppleBinding.create(subject, encryptedRefreshToken);
        account.profileHints = new ProfileHints(fullName, null, email);
        return account;
    }

    /** 同一 Apple 身份再次登录时轮换服务端保存的 refresh token 密文。 */
    public void rotateAppleRefreshToken(String subject, String encryptedRefreshToken) {
        if (appleBinding == null || !MessageDigest.isEqual(
                appleBinding.getSubject().getBytes(StandardCharsets.UTF_8),
                subject.getBytes(StandardCharsets.UTF_8))) {
            throw AuthErrorCode.APPLE_IDENTITY_INVALID.toDomainException();
        }
        appleBinding = appleBinding.rotateRefreshToken(encryptedRefreshToken);
    }

    /**
     * 通过微信网页（PC 扫码）登录创建账号。
     */
    public static Account createFromWechatWeb(String webOpenid, String unionid,
                                              String nickname, String avatar) {
        if (webOpenid == null || webOpenid.isBlank()) {
            throw AuthErrorCode.WEB_OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wxweb_" + shortHash(webOpenid);
        account.password = DISABLED_PASSWORD;
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.createForWeb(webOpenid, unionid);
        account.profileHints = ProfileHints.ofWechat(nickname, avatar);
        return account;
    }

    /**
     * 通过微信公众号 H5 登录创建账号。
     */
    public static Account createFromWechatH5(String mpOpenid, String unionid,
                                             String nickname, String avatar) {
        if (mpOpenid == null || mpOpenid.isBlank()) {
            throw AuthErrorCode.MP_OPENID_REQUIRED.toDomainException();
        }
        Account account = new Account();
        account.username = "wxmp_" + shortHash(mpOpenid);
        account.password = DISABLED_PASSWORD;
        account.status = AccountStatus.ACTIVE;
        account.wechatBinding = WechatBinding.createForH5(mpOpenid, unionid);
        account.profileHints = ProfileHints.ofWechat(nickname, avatar);
        return account;
    }

    /**
     * 取 openid 的 SHA-256 哈希前 16 字符（小写 hex）作为 username 后缀。
     * 单纯截原始 openid 前 16 字符在多个 openid 共享前缀时会触发 username unique 冲突。
     */
    private static String shortHash(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ==================== 事件发布 ====================

    /**
     * JPA 持久化回调：首次 INSERT 后自动注册 AccountRegisteredEvent。
     *
     * <p>此时 ID 已由数据库分配（{@code GenerationType.IDENTITY}），事件携带完整的 accountId。
     * Spring Data 在 {@code save()} 返回前读取已注册的领域事件并发布。
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
     * 发布账号删除事件（跨域事件，system 域级联删除 User）。
     */
    public void publishDeletedEvent() {
        registerEvent(new AccountDeletedEvent(getId()));
    }

    // ==================== 凭据管理 ====================

    /**
     * 修改密码。
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
     * 当前账号是否禁用密码登录（密码字段为 {@link #DISABLED_PASSWORD}）。
     */
    public boolean isPasswordLoginDisabled() {
        return DISABLED_PASSWORD.equals(this.password);
    }

    /**
     * 绑定手机号（微信登录后补充手机号场景）。
     *
     * <p>username 不随手机号变化——用户名是登录别名，与手机号脱钩，避免与已有用户名冲突。
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

    /**
     * 修改手机号（替换已绑定的手机号，App 用户自助改号场景）。
     *
     * <p>与 {@link #bindPhone(String)} 区别：允许覆盖已有手机号，并注册
     * {@link AccountPhoneChangedEvent} 供下游同步。新号与旧号是否相同的判定在应用服务层。
     *
     * @param phone 新手机号
     */
    public void changePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw AuthErrorCode.PHONE_REQUIRED.toDomainException();
        }
        this.phone = phone;
        registerEvent(new AccountPhoneChangedEvent(getId(), phone));
    }

    // ==================== 冻结 / 解冻 ====================

    /**
     * 管理员冻结账号。
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
     * 管理员解冻账号。
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
     * 尝试自动解冻（到期自动解冻）。
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

    // ==================== 微信绑定 ====================

    /**
     * 绑定微信小程序。
     *
     * <p>本渠道已绑相同 openid 幂等；已绑不同 openid 或 unionid 不同主体
     * 抛 {@code WECHAT_ALREADY_BOUND}；不清除其他渠道 openid。
     */
    public void bindWechat(String openid, String unionid) {
        if (openid == null || openid.isBlank()) {
            throw AuthErrorCode.OPENID_REQUIRED.toDomainException();
        }
        ensureWechatIdentityCompatible(this.wechatBinding == null
                ? null : this.wechatBinding.getOpenid(), openid, unionid);
        if (this.wechatBinding == null) {
            this.wechatBinding = WechatBinding.create(openid, unionid);
        } else {
            this.wechatBinding = this.wechatBinding.withOpenid(openid);
            if (unionid != null) {
                this.wechatBinding = this.wechatBinding.withUnionid(unionid);
            }
        }
    }

    /**
     * 绑定微信网页（PC 扫码）。冲突语义同 {@link #bindWechat}。
     */
    public void bindWechatWeb(String webOpenid, String unionid) {
        if (webOpenid == null || webOpenid.isBlank()) {
            throw AuthErrorCode.WEB_OPENID_REQUIRED.toDomainException();
        }
        ensureWechatIdentityCompatible(this.wechatBinding == null
                ? null : this.wechatBinding.getWebOpenid(), webOpenid, unionid);
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
     * 绑定微信公众号 H5。冲突语义同 {@link #bindWechat}。
     */
    public void bindWechatH5(String mpOpenid, String unionid) {
        if (mpOpenid == null || mpOpenid.isBlank()) {
            throw AuthErrorCode.MP_OPENID_REQUIRED.toDomainException();
        }
        ensureWechatIdentityCompatible(this.wechatBinding == null
                ? null : this.wechatBinding.getMpOpenid(), mpOpenid, unionid);
        if (this.wechatBinding == null) {
            this.wechatBinding = WechatBinding.createForH5(mpOpenid, unionid);
        } else {
            this.wechatBinding = this.wechatBinding.withMpOpenid(mpOpenid);
            if (unionid != null) {
                this.wechatBinding = this.wechatBinding.withUnionid(unionid);
            }
        }
    }

    /**
     * 微信身份兼容性检查：同渠道异 openid、或 unionid 不同主体，都视为
     * 「本账号已绑定其他微信」。
     *
     * @param channelOpenid 本渠道已绑 openid（可空）
     * @param newOpenid     新 openid
     * @param newUnionid    新 unionid（可空）
     */
    private void ensureWechatIdentityCompatible(String channelOpenid,
                                                String newOpenid, String newUnionid) {
        if (channelOpenid != null && !channelOpenid.equals(newOpenid)) {
            throw AuthErrorCode.WECHAT_ALREADY_BOUND.toDomainException();
        }
        if (this.wechatBinding != null
                && this.wechatBinding.getUnionid() != null
                && newUnionid != null
                && !this.wechatBinding.getUnionid().equals(newUnionid)) {
            throw AuthErrorCode.WECHAT_ALREADY_BOUND.toDomainException();
        }
    }

    // ==================== Apple 绑定 ====================

    /**
     * 绑定 Apple 身份（social_bind 挂接到手机号主账号场景）。
     *
     * <p>已绑相同 subject 幂等并轮换 refresh token 密文；
     * 已绑不同 subject 抛 {@code APPLE_ALREADY_BOUND}。
     */
    public void bindApple(String subject, String encryptedRefreshToken) {
        if (subject == null || subject.isBlank()) {
            throw AuthErrorCode.APPLE_SUBJECT_REQUIRED.toDomainException();
        }
        if (this.appleBinding != null) {
            if (!this.appleBinding.getSubject().equals(subject)) {
                throw AuthErrorCode.APPLE_ALREADY_BOUND.toDomainException();
            }
            this.appleBinding = this.appleBinding.rotateRefreshToken(encryptedRefreshToken);
            return;
        }
        this.appleBinding = AppleBinding.create(subject, encryptedRefreshToken);
    }

    // ==================== 淘宝绑定 ====================

    /**
     * 绑定淘宝账号（淘宝登录补绑手机号后挂接）。
     *
     * <p>已绑相同 openUid 幂等返回；绑不同 openUid 抛 {@code TAOBAO_ALREADY_BOUND}。
     */
    public void bindTaobao(String openUid) {
        if (openUid == null || openUid.isBlank()) {
            throw AuthErrorCode.TAOBAO_AUTH_REQUIRED.toDomainException();
        }
        if (this.taobaoBinding != null && this.taobaoBinding.getOpenUid() != null
                && !this.taobaoBinding.getOpenUid().equals(openUid)) {
            throw AuthErrorCode.TAOBAO_ALREADY_BOUND.toDomainException();
        }
        this.taobaoBinding = TaobaoBinding.create(openUid);
    }
}

package com.eagle.system.auth.application.service;

import com.eagle.common.exception.codes.DataErrorCode;
import com.eagle.system.auth.application.command.FreezeAccountCommand;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.model.valueobject.ProfileHints;
import com.eagle.system.auth.domain.repository.AccountRepository;
import com.eagle.system.auth.domain.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 账号应用服务。
 *
 * <p>管理 Account 聚合根的完整生命周期。所有认证凭据操作（注册、密码、冻结、删除）
 * 在此处完成，不暴露 AccountRepository 给其他模块。
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    /**
     * 短信验证码 Web 登录：校验手机号格式 + 验证码后查 / 建账号。
     *
     * <p>校验失败抛出 {@link com.eagle.common.exception.DomainException}，由全局异常处理。
     */
    @Transactional(rollbackFor = Exception.class)
    public Account authenticateBySmsCode(String phone, String code) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw DataErrorCode.INVALID_PHONE_FORMAT.toDomainException();
        }
        if (!smsService.verifyCode(phone, code)) {
            throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
        }
        return findOrCreateByPhone(phone);
    }

    /**
     * 用户自主注册。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String rawPassword, String phone,
                         String email, String nickname) {
        return createAccountInternal(username, rawPassword, phone, nickname, null, email);
    }

    /**
     * 管理员创建账号。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(String username, String rawPassword, String phone,
                              String nickname, String name, String email) {
        return createAccountInternal(username, rawPassword, phone, nickname, name, email);
    }

    private Long createAccountInternal(String username, String rawPassword, String phone,
                                       String nickname, String name, String email) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw AuthErrorCode.ACCOUNT_ALREADY_EXISTS.toConflictException();
        }
        if (phone != null && !phone.isBlank()) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw DataErrorCode.INVALID_PHONE_FORMAT.toDomainException();
            }
            accountRepository.findByPhone(phone).ifPresent(a -> {
                throw AuthErrorCode.PHONE_ALREADY_BOUND.toConflictException();
            });
        }
        ProfileHints hints = new ProfileHints(nickname, null, email);
        Account account = Account.create(
                username, passwordEncoder.encode(rawPassword), phone, hints);
        return accountRepository.save(account).getId();
    }

    /**
     * 修改密码。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long accountId, String rawNewPassword) {
        Account account = findAccountById(accountId);
        account.changePassword(passwordEncoder.encode(rawNewPassword));
        accountRepository.save(account);
    }

    /**
     * 冻结账号（管理员显式触发）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void freezeAccount(Long accountId, FreezeAccountCommand cmd) {
        Account account = findAccountById(accountId);
        account.freezeByAdmin(cmd.operatorId(), cmd.operatorName(),
                cmd.reason(), cmd.freezeUntil(), cmd.remark());
        accountRepository.save(account);
    }

    /**
     * 解冻账号（管理员显式触发）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAccount(Long accountId, Long operatorId, String operatorName) {
        Account account = findAccountById(accountId);
        account.unfreeze(operatorId, operatorName);
        accountRepository.save(account);
    }

    /**
     * 删除账号（发布 AccountDeletedEvent，system 域异步级联删除 User）。
     *
     * <p>使用 {@link AccountRepository#delete} 配合 Spring Data 的 {@code @DomainEvents}
     * 机制：聚合根上注册的事件在删除事务提交后自动发布，无需手工 save。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long accountId) {
        Account account = findAccountById(accountId);
        account.publishDeletedEvent();
        accountRepository.delete(account);
    }

    /**
     * 按手机号查找或自动创建账号（短信验证码登录场景）。
     *
     * @param phone 手机号
     * @return Account 实例
     */
    @Transactional(rollbackFor = Exception.class)
    public Account findOrCreateByPhone(String phone) {
        return accountRepository.findByPhone(phone)
                .orElseGet(() -> {
                    Account newAccount = Account.createFromPhone(phone);
                    return accountRepository.save(newAccount);
                });
    }

    /**
     * 按微信小程序 openid 查找或自动创建账号。
     *
     * @param openid  微信小程序 openid
     * @param unionid 微信 unionid（可选）
     * @return Account 实例
     */
    @Transactional(rollbackFor = Exception.class)
    public Account findOrCreateByWechatOpenid(String openid, String unionid) {
        return accountRepository.findByWechatBindingOpenid(openid)
                .orElseGet(() -> {
                    Account newAccount = Account.createFromWechat(openid, unionid);
                    return accountRepository.save(newAccount);
                });
    }

    /**
     * 通过短信验证码重置密码（忘记密码场景，未认证）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByPhone(String phone, String code, String rawNewPassword) {
        if (!smsService.verifyCode(phone, code)) {
            throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
        }
        Account account = accountRepository.findByPhone(phone)
                .orElseThrow(AuthErrorCode.PHONE_NOT_BOUND::toNotFoundException);
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        account.changePassword(passwordEncoder.encode(rawNewPassword));
        accountRepository.save(account);
    }

    /**
     * 绑定手机号（微信登录后补充手机号场景，已认证）。
     *
     * <p>注意：username 字段不随手机号变化（用户名是登录别名，与手机号脱钩），
     * 微信账号绑手机号后 username 仍是创建时生成的 wx_xxx，避免与已有用户名冲突。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(Long accountId, String phone, String code) {
        if (!smsService.verifyCode(phone, code)) {
            throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
        }
        accountRepository.findByPhone(phone).ifPresent(existing -> {
            if (!existing.getId().equals(accountId)) {
                throw AuthErrorCode.PHONE_ALREADY_BOUND.toConflictException();
            }
        });
        Account account = findAccountById(accountId);
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        account.bindPhone(phone);
        accountRepository.save(account);
    }

    /**
     * 发送找回密码验证码（验证手机号已绑定账号后发送）。
     */
    @Transactional(readOnly = true)
    public void sendResetCode(String phone) {
        accountRepository.findByPhone(phone)
                .orElseThrow(AuthErrorCode.PHONE_NOT_BOUND::toNotFoundException);
        smsService.sendCode(phone);
    }

    private Account findAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);
    }
}

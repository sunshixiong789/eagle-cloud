package com.eagle.system.auth.application.service;

import com.eagle.common.exception.codes.DataErrorCode;
import com.eagle.system.auth.application.command.FreezeAccountCommand;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.model.valueobject.ProfileHints;
import com.eagle.system.auth.domain.repository.AccountRepository;
import com.eagle.system.auth.domain.service.SmsService;
import com.eagle.system.auth.domain.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 账号应用服务
 * <p>
 * 管理 Account 聚合根的完整生命周期。
 * 所有认证凭据操作（注册、密码、锁定、删除）在此处完成，
 * 不暴露 AccountRepository 给其他模块。
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
     * 短信验证码 Web 登录：校验手机号格式 + 验证码后查/建账号。
     * <p>校验失败抛出 {@link com.eagle.common.exception.DomainException}，由全局异常处理。</p>
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
     * 用户自主注册
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String rawPassword, String phone,
                         String email, String nickname) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw AuthErrorCode.ACCOUNT_ALREADY_EXISTS.toConflictException();
        }
        ProfileHints hints = new ProfileHints(nickname, null, email);
        Account account = Account.create(
                username, passwordEncoder.encode(rawPassword), phone, hints);
        return accountRepository.save(account).getId();
    }

    /**
     * 管理员创建账号
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(String username, String rawPassword, String phone,
                              String nickname, String name, String email) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw AuthErrorCode.ACCOUNT_ALREADY_EXISTS.toConflictException();
        }
        ProfileHints hints = new ProfileHints(nickname, null, email);
        Account account = Account.create(
                username, passwordEncoder.encode(rawPassword), phone, hints);
        return accountRepository.save(account).getId();
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long accountId, String rawNewPassword) {
        Account account = findAccountById(accountId);
        account.changePassword(passwordEncoder.encode(rawNewPassword));
        accountRepository.save(account);
    }

    /** 冻结账号（管理员显式触发）*/
    @Transactional(rollbackFor = Exception.class)
    public void freezeAccount(Long accountId, FreezeAccountCommand cmd) {
        Account account = findAccountById(accountId);
        account.freezeByAdmin(cmd.operatorId(), cmd.operatorName(),
                cmd.reason(), cmd.freezeUntil(), cmd.remark());
        accountRepository.save(account);
    }

    /** 解冻账号（管理员显式触发）*/
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAccount(Long accountId, Long operatorId, String operatorName) {
        Account account = findAccountById(accountId);
        account.unfreeze(operatorId, operatorName);
        accountRepository.save(account);
    }

    /**
     * @deprecated 改用 {@link #freezeAccount}
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void lockAccount(Long accountId) {
        Account account = findAccountById(accountId);
        account.lock();
        accountRepository.save(account);
    }

    /**
     * @deprecated 改用 {@link #unfreezeAccount}
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void unlockAccount(Long accountId) {
        Account account = findAccountById(accountId);
        account.unlock();
        accountRepository.save(account);
    }

    /**
     * 删除账号（发布 AccountDeletedEvent，system 域异步级联删除 User）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long accountId) {
        Account account = findAccountById(accountId);
        account.publishDeletedEvent();
        accountRepository.save(account);  // flush event before delete
        accountRepository.deleteById(accountId);
    }

    /**
     * 按手机号查找或自动创建账号（短信验证码登录场景）
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
     * 按微信小程序 openid 查找或自动创建账号
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
     * 通过短信验证码重置密码(忘记密码场景,未认证)
     * <p>
     * 业务流程:
     * <ol>
     *   <li>验证短信验证码是否有效</li>
     *   <li>查找手机号对应的账号</li>
     *   <li>检查账号是否被锁定</li>
     *   <li>更新密码</li>
     * </ol>
     *
     * @param phone          手机号
     * @param code           短信验证码
     * @param rawNewPassword 新密码(明文)
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByPhone(String phone, String code, String rawNewPassword) {
        // 1. 验证短信验证码
        if (!smsService.verifyCode(phone, code)) {
            throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
        }
        // 2. 查找账号
        Account account = accountRepository.findByPhone(phone)
                .orElseThrow(AuthErrorCode.PHONE_NOT_BOUND::toNotFoundException);
        // 3. 检查账号状态
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        // 4. 更新密码
        account.changePassword(passwordEncoder.encode(rawNewPassword));
        accountRepository.save(account);
    }

    /**
     * 绑定手机号(微信登录后补充手机号场景,已认证)
     * <p>
     * 业务规则:
     * <ul>
     *   <li>验证短信验证码</li>
     *   <li>检查手机号是否已被其他账号绑定</li>
     *   <li>检查账号是否被锁定</li>
     *   <li>绑定手机号</li>
     * </ul>
     *
     * @param accountId 当前登录账号 ID
     * @param phone     手机号
     * @param code      短信验证码
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(Long accountId, String phone, String code) {
        // 1. 验证短信验证码
        if (!smsService.verifyCode(phone, code)) {
            throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
        }
        // 2. 检查手机号是否已被其他账号绑定
        accountRepository.findByPhone(phone).ifPresent(existing -> {
            if (!existing.getId().equals(accountId)) {
                throw AuthErrorCode.PHONE_ALREADY_BOUND.toConflictException();
            }
        });
        // 3. 查找当前账号
        Account account = findAccountById(accountId);
        // 4. 检查账号状态
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        // 5. 绑定手机号
        account.bindPhone(phone);
        accountRepository.save(account);
    }

    /**
     * 发送找回密码验证码（验证手机号已绑定账号后发送）
     *
     * @param phone 手机号
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

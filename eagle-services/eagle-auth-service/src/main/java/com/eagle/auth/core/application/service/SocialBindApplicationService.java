package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.SmsService;
import com.eagle.auth.core.infrastructure.security.BlacklistChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 第三方身份挂靠手机号应用服务（social_bind grant 核心逻辑）。
 *
 * <p>流程：短信验证码 → 消费 BindTicket → 黑名单 → 查/建手机号主账号 →
 * 冻结前置检查（挂接是持久化副作用，必须在绑定之前拒绝）→ 挂接第三方身份 → 保存。
 * 验证码校验必须先于 ticket 消费：验证码输错是高频可重试事件，
 * 若先 GETDEL ticket 会被一次错码烧掉，用户被迫重走第三方授权。
 * 并发兜底由第三方身份唯一索引兜住，唯一约束冲突翻译为
 * {@link AuthErrorCode#SOCIAL_IDENTITY_ALREADY_BOUND}。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialBindApplicationService {

    private final BindTicketStore bindTicketStore;
    private final SmsService smsService;
    private final AccountRepository accountRepository;
    private final AccountApplicationService accountApplicationService;
    private final BlacklistChecker blacklistChecker;

    /**
     * 消费 bind_ticket，把第三方身份挂靠到手机号主账号。
     *
     * @param ticketId binding_required 响应下发的一次性凭证
     * @param phone    手机号
     * @param code     短信验证码
     * @param clientIp 客户端 IP（黑名单检查）
     * @return 挂接完成的主账号
     */
    @Transactional(rollbackFor = Exception.class)
    public Account bind(String ticketId, String phone, String code, String clientIp) {
        if (!smsService.verifyCode(phone, code)) {
            throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
        }
        BindTicket ticket = bindTicketStore.consume(ticketId)
                .orElseThrow(AuthErrorCode.SOCIAL_BIND_TICKET_INVALID::toDomainException);
        checkBlacklist(ticket, phone, clientIp);

        Account account = accountApplicationService.findOrCreateByPhone(phone, hintsOf(ticket));
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw AuthErrorCode.ACCOUNT_FROZEN.toDomainException();
        }
        ensureIdentityNotBoundElsewhere(ticket, account);
        attach(account, ticket);
        return saveWithBindingUniquenessGuard(account);
    }

    private void checkBlacklist(BindTicket ticket, String phone, String clientIp) {
        blacklistChecker.checkLogin(null, phone, clientIp, null);
        switch (ticket.provider()) {
            case TAOBAO -> blacklistChecker.checkTaobao(ticket.identifier(), clientIp);
            case APPLE -> blacklistChecker.checkApple(ticket.identifier(), clientIp);
            case WECHAT -> blacklistChecker.checkWechat(ticket.identifier(), clientIp);
        }
    }

    /**
     * 并发兜底预检：ticket 签发到消费之间，同一第三方身份可能已被其他账号挂接
     * （双端并发提交）。已挂到当前主账号视为幂等重放，放行。
     */
    private void ensureIdentityNotBoundElsewhere(BindTicket ticket, Account account) {
        findByIdentity(ticket).ifPresent(bound -> {
            if (!bound.getId().equals(account.getId())) {
                throw AuthErrorCode.SOCIAL_IDENTITY_ALREADY_BOUND.toConflictException();
            }
        });
    }

    private Optional<Account> findByIdentity(BindTicket ticket) {
        return switch (ticket.provider()) {
            case TAOBAO -> accountRepository.findByTaobaoBindingOpenUid(ticket.identifier());
            case APPLE -> accountRepository.findByAppleBindingSubject(ticket.identifier());
            case WECHAT -> switch (ticket.wechatChannel()) {
                case MINI_PROGRAM -> accountRepository.findByWechatBindingOpenid(ticket.identifier());
                case APP, PC -> accountRepository.findByWechatBindingWebOpenid(ticket.identifier());
                case H5 -> accountRepository.findByWechatBindingMpOpenid(ticket.identifier());
            };
        };
    }

    private void attach(Account account, BindTicket ticket) {
        switch (ticket.provider()) {
            case TAOBAO -> account.bindTaobao(ticket.identifier());
            case APPLE -> account.bindApple(
                    ticket.identifier(), ticket.appleRefreshTokenCiphertext());
            case WECHAT -> {
                switch (ticket.wechatChannel()) {
                    case MINI_PROGRAM -> account.bindWechat(ticket.identifier(), ticket.unionid());
                    case APP, PC -> account.bindWechatWeb(ticket.identifier(), ticket.unionid());
                    case H5 -> account.bindWechatH5(ticket.identifier(), ticket.unionid());
                }
            }
        }
    }

    private ProfileHints hintsOf(BindTicket ticket) {
        return switch (ticket.provider()) {
            case TAOBAO -> ProfileHints.EMPTY;
            case APPLE -> new ProfileHints(ticket.appleFullName(), null, ticket.appleEmail());
            case WECHAT -> ProfileHints.ofWechat(ticket.nickname(), ticket.avatar());
        };
    }

    /**
     * 保存主账号并把第三方身份唯一索引冲突翻译为业务异常（TOCTOU 并发兜底）。
     */
    private Account saveWithBindingUniquenessGuard(Account account) {
        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException ex) {
            log.warn("social binding uniqueness violation on save, accountId={}",
                    account.getId(), ex);
            throw AuthErrorCode.SOCIAL_IDENTITY_ALREADY_BOUND.toConflictException();
        }
    }
}

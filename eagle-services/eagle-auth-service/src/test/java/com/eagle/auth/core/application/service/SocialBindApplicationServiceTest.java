package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.FreezeReason;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.SmsService;
import com.eagle.auth.core.infrastructure.security.BlacklistChecker;
import com.eagle.common.exception.AppException;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialBindApplicationServiceTest {

    private static final String TICKET_ID = "ticket-1";
    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";
    private static final String IP = "1.2.3.4";

    @Mock
    BindTicketStore bindTicketStore;
    @Mock
    SmsService smsService;
    @Mock
    AccountRepository accountRepository;
    @Mock
    AccountApplicationService accountApplicationService;
    @Mock
    BlacklistChecker blacklistChecker;
    @InjectMocks
    SocialBindApplicationService service;

    private Account phoneAccount(Long id) {
        Account account = Account.createFromPhone(PHONE);
        // BaseAggregateRoot 的 id 由 JPA 分配，测试用反射注入
        try {
            var field = findIdField(account.getClass());
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return account;
    }

    private static java.lang.reflect.Field findIdField(Class<?> type)
            throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField("id");
            } catch (NoSuchFieldException ignored) {
                // 继续向上找
            }
        }
        throw new NoSuchFieldException("id");
    }

    @Test
    @DisplayName("ticket 无效应抛 SOCIAL_BIND_TICKET_INVALID")
    void invalidTicketShouldThrow() {
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(bindTicketStore.consume(TICKET_ID)).thenReturn(Optional.empty());

        AppException ex = assertThrows(DomainException.class,
                () -> service.bind(TICKET_ID, PHONE, CODE, IP));

        assertEquals(AuthErrorCode.SOCIAL_BIND_TICKET_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("验证码错误应抛 SMS_CODE_INVALID，且不消费 ticket（可重试）")
    void invalidSmsCodeShouldThrowWithoutConsumingTicket() {
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(false);

        AppException ex = assertThrows(DomainException.class,
                () -> service.bind(TICKET_ID, PHONE, CODE, IP));

        assertEquals(AuthErrorCode.SMS_CODE_INVALID, ex.getErrorCode());
        // 验证码输错是高频事件，ticket 必须保留供用户原地重试
        verify(bindTicketStore, never()).consume(TICKET_ID);
    }

    @Test
    @DisplayName("淘宝身份应挂接到手机号主账号并保存")
    void taobaoShouldAttachToPhoneAccount() {
        Account account = phoneAccount(1L);
        when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofTaobao("tb-1")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(PHONE, ProfileHints.EMPTY))
                .thenReturn(account);
        when(accountRepository.findByTaobaoBindingOpenUid("tb-1")).thenReturn(Optional.empty());
        when(accountRepository.save(account)).thenReturn(account);

        Account result = service.bind(TICKET_ID, PHONE, CODE, IP);

        assertSame(account, result);
        assertEquals("tb-1", result.getTaobaoBinding().getOpenUid());
        verify(blacklistChecker).checkLogin(null, PHONE, IP, null);
        verify(blacklistChecker).checkTaobao("tb-1", IP);
    }

    @Test
    @DisplayName("Apple 身份应携带 email/fullName hints 创建主账号并挂接密文")
    void appleShouldAttachWithHints() {
        Account account = phoneAccount(1L);
        when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofApple(
                        "sub-1", "a@b.com", "小明", "cipher-1")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(
                PHONE, new ProfileHints("小明", null, "a@b.com"))).thenReturn(account);
        when(accountRepository.findByAppleBindingSubject("sub-1")).thenReturn(Optional.empty());
        when(accountRepository.save(account)).thenReturn(account);

        Account result = service.bind(TICKET_ID, PHONE, CODE, IP);

        assertEquals("sub-1", result.getAppleBinding().getSubject());
        assertEquals("cipher-1", result.getAppleBinding().getRefreshTokenCiphertext());
    }

    @Test
    @DisplayName("微信小程序身份应按渠道挂接 openid+unionid")
    void wechatMiniProgramShouldAttachByChannel() {
        Account account = phoneAccount(1L);
        when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofWechat(
                        WechatChannel.MINI_PROGRAM, "oid-1", "uid-1", "Nick", "http://a.png")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(
                PHONE, ProfileHints.ofWechat("Nick", "http://a.png"))).thenReturn(account);
        when(accountRepository.findByWechatBindingOpenid("oid-1")).thenReturn(Optional.empty());
        when(accountRepository.save(account)).thenReturn(account);

        Account result = service.bind(TICKET_ID, PHONE, CODE, IP);

        assertEquals("oid-1", result.getWechatBinding().getOpenid());
        assertEquals("uid-1", result.getWechatBinding().getUnionid());
        verify(blacklistChecker).checkWechat("oid-1", IP);
    }

    @Test
    @DisplayName("主账号被冻结应在挂接前拒绝")
    void frozenAccountShouldRejectBeforeAttach() {
        Account account = phoneAccount(1L);
        account.freezeByAdmin(9L, "admin", FreezeReason.RISK_CONTROL,
                LocalDateTime.now().plusDays(1), "risk");
        when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofTaobao("tb-1")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(eq(PHONE), any()))
                .thenReturn(account);

        AppException ex = assertThrows(DomainException.class,
                () -> service.bind(TICKET_ID, PHONE, CODE, IP));

        assertEquals(AuthErrorCode.ACCOUNT_FROZEN, ex.getErrorCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("身份已被其他账号挂接应抛 SOCIAL_IDENTITY_ALREADY_BOUND（并发兜底）")
    void identityBoundElsewhereShouldConflict() {
        Account mine = phoneAccount(1L);
        Account other = phoneAccount(2L);
        when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofTaobao("tb-1")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(eq(PHONE), any())).thenReturn(mine);
        when(accountRepository.findByTaobaoBindingOpenUid("tb-1"))
                .thenReturn(Optional.of(other));

        AppException ex = assertThrows(ConflictException.class,
                () -> service.bind(TICKET_ID, PHONE, CODE, IP));

        assertEquals(AuthErrorCode.SOCIAL_IDENTITY_ALREADY_BOUND, ex.getErrorCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("身份已挂到当前主账号视为幂等重放，正常返回")
    void identityBoundToSameAccountIsIdempotent() {
        Account mine = phoneAccount(1L);
        mine.bindTaobao("tb-1");
        when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofTaobao("tb-1")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(eq(PHONE), any())).thenReturn(mine);
        when(accountRepository.findByTaobaoBindingOpenUid("tb-1"))
                .thenReturn(Optional.of(mine));
        when(accountRepository.save(mine)).thenReturn(mine);

        Account result = service.bind(TICKET_ID, PHONE, CODE, IP);

        assertSame(mine, result);
    }

    @Test
    @DisplayName("保存时唯一索引冲突应翻译为 SOCIAL_IDENTITY_ALREADY_BOUND")
    void uniquenessViolationOnSaveShouldConflict() {
        Account account = phoneAccount(1L);
        lenient().when(bindTicketStore.consume(TICKET_ID))
                .thenReturn(Optional.of(BindTicket.ofTaobao("tb-1")));
        when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone(eq(PHONE), any())).thenReturn(account);
        when(accountRepository.findByTaobaoBindingOpenUid("tb-1")).thenReturn(Optional.empty());
        when(accountRepository.save(account))
                .thenThrow(new DataIntegrityViolationException("dup"));

        AppException ex = assertThrows(ConflictException.class,
                () -> service.bind(TICKET_ID, PHONE, CODE, IP));

        assertEquals(AuthErrorCode.SOCIAL_IDENTITY_ALREADY_BOUND, ex.getErrorCode());
    }
}

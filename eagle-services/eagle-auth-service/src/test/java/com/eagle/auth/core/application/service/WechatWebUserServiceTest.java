package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.WechatWebService.WechatWebUserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatWebUserServiceTest {

    private static final String OPENID = "wx_openid_abcdef0123456789";
    private static final String UNIONID = "wx_unionid_xyz";

    @Mock
    AccountRepository accountRepository;
    @InjectMocks
    WechatWebUserService service;

    private WechatWebUserInfo info(String channel, String openid, String unionid) {
        return new WechatWebUserInfo(openid, unionid, "Nick", "https://a.png", channel);
    }

    @Nested
    @DisplayName("findOrCreateWechatWebAccount")
    class FindOrCreate {

        @Test
        @DisplayName("应Merge通过unionid")
        void shouldMergeByUnionid() {
            Account existing = Account.createFromPhone("13800138000");
            when(accountRepository.findByWechatBindingUnionid(UNIONID)).thenReturn(Optional.of(existing));
            when(accountRepository.save(existing)).thenReturn(existing);

            Account result = service.findOrCreateWechatWebAccount(info("pc", OPENID, UNIONID));

            assertSame(existing, result);
            assertNotNull(existing.getWechatBinding());
            assertEquals(OPENID, existing.getWechatBinding().getWebOpenid());
        }

        @Test
        @DisplayName("unionid不存在时应查找通过Pcopenid")
        void shouldFindByPcOpenidWhenUnionidAbsent() {
            Account existing = Account.createFromWechatWeb(OPENID, null, "Nick", null);
            when(accountRepository.findByWechatBindingWebOpenid(OPENID)).thenReturn(Optional.of(existing));

            Account result = service.findOrCreateWechatWebAccount(info("pc", OPENID, null));

            assertSame(existing, result);
            // no save when unionid not provided
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("应查找通过小时5openid")
        void shouldFindByH5Openid() {
            Account existing = Account.createFromWechatH5(OPENID, null, "Nick", null);
            when(accountRepository.findByWechatBindingMpOpenid(OPENID)).thenReturn(Optional.of(existing));

            Account result = service.findOrCreateWechatWebAccount(info("h5", OPENID, null));

            assertSame(existing, result);
        }

        @Test
        @DisplayName("应创建NewPc账号")
        void shouldCreateNewPcAccount() {
            when(accountRepository.findByWechatBindingUnionid(UNIONID)).thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingWebOpenid(OPENID)).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            Account result = service.findOrCreateWechatWebAccount(info("pc", OPENID, UNIONID));

            assertNotNull(result);
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertEquals(OPENID, captor.getValue().getWechatBinding().getWebOpenid());
        }

        @Test
        @DisplayName("应创建New小时5账号")
        void shouldCreateNewH5Account() {
            when(accountRepository.findByWechatBindingUnionid(any())).thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingMpOpenid(OPENID)).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            Account result = service.findOrCreateWechatWebAccount(info("h5", OPENID, UNIONID));

            assertEquals(OPENID, result.getWechatBinding().getMpOpenid());
        }
    }

    @Nested
    @DisplayName("findWechatAccount 四渠道统一查找（不创建）")
    class FindWechatAccount {

        @Test
        @DisplayName("小程序渠道 openid 命中应直接返回")
        void miniProgramOpenidHit() {
            Account existing = Account.createFromWechat(OPENID, UNIONID);
            when(accountRepository.findByWechatBindingOpenid(OPENID))
                    .thenReturn(Optional.of(existing));

            Optional<Account> result = service.findWechatAccount(
                    WechatChannel.MINI_PROGRAM, OPENID, UNIONID);

            assertSame(existing, result.orElseThrow());
        }

        @Test
        @DisplayName("小程序 openid 未命中但 unionid 命中，应补绑小程序 openid 并返回")
        void miniProgramUnionidHitShouldAttachOpenid() {
            Account existing = Account.createFromPhone("13800138000");
            existing.bindWechatWeb("web-openid-1", UNIONID);
            when(accountRepository.findByWechatBindingOpenid(OPENID))
                    .thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingUnionid(UNIONID))
                    .thenReturn(Optional.of(existing));
            when(accountRepository.save(existing)).thenReturn(existing);

            Optional<Account> result = service.findWechatAccount(
                    WechatChannel.MINI_PROGRAM, OPENID, UNIONID);

            assertSame(existing, result.orElseThrow());
            assertEquals(OPENID, existing.getWechatBinding().getOpenid());
            assertEquals("web-openid-1", existing.getWechatBinding().getWebOpenid());
            verify(accountRepository).save(existing);
        }

        @Test
        @DisplayName("openid 与 unionid 均未命中应返回 empty 且不创建账号")
        void bothMissShouldReturnEmpty() {
            when(accountRepository.findByWechatBindingOpenid(OPENID))
                    .thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingUnionid(UNIONID))
                    .thenReturn(Optional.empty());

            Optional<Account> result = service.findWechatAccount(
                    WechatChannel.MINI_PROGRAM, OPENID, UNIONID);

            assertEquals(Optional.empty(), result);
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("无 unionid 时仅按本渠道 openid 查找")
        void withoutUnionidOnlyChannelLookup() {
            when(accountRepository.findByWechatBindingMpOpenid(OPENID))
                    .thenReturn(Optional.empty());

            Optional<Account> result = service.findWechatAccount(
                    WechatChannel.H5, OPENID, null);

            assertEquals(Optional.empty(), result);
            verify(accountRepository, never()).findByWechatBindingUnionid(any());
        }
    }
}

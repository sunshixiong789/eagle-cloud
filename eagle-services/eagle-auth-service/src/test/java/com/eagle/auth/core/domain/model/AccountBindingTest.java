package com.eagle.auth.core.domain.model;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Account 第三方绑定冲突语义测试（手机号为主账号体系）。
 */
class AccountBindingTest {

    private Account phoneAccount() {
        return Account.createFromPhone("13800138000");
    }

    @Nested
    @DisplayName("bindApple")
    class BindApple {

        @Test
        @DisplayName("未绑定时应挂接 Apple 身份")
        void shouldBindWhenAbsent() {
            Account account = phoneAccount();
            account.bindApple("sub-1", "cipher-1");
            assertEquals("sub-1", account.getAppleBinding().getSubject());
            assertEquals("cipher-1", account.getAppleBinding().getRefreshTokenCiphertext());
        }

        @Test
        @DisplayName("重复绑定相同 subject 应幂等并轮换密文")
        void shouldRotateOnSameSubject() {
            Account account = phoneAccount();
            account.bindApple("sub-1", "cipher-1");
            account.bindApple("sub-1", "cipher-2");
            assertEquals("cipher-2", account.getAppleBinding().getRefreshTokenCiphertext());
        }

        @Test
        @DisplayName("已绑不同 subject 应抛 APPLE_ALREADY_BOUND")
        void shouldRejectDifferentSubject() {
            Account account = phoneAccount();
            account.bindApple("sub-1", "cipher-1");
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.bindApple("sub-2", "cipher-2"));
            assertEquals(AuthErrorCode.APPLE_ALREADY_BOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("bindWechat 系列冲突语义")
    class BindWechatConflict {

        @Test
        @DisplayName("小程序渠道已绑不同 openid 应抛 WECHAT_ALREADY_BOUND")
        void miniProgramConflict() {
            Account account = phoneAccount();
            account.bindWechat("oid-1", "uid-1");
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.bindWechat("oid-2", "uid-1"));
            assertEquals(AuthErrorCode.WECHAT_ALREADY_BOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("小程序渠道相同 openid 应幂等")
        void miniProgramIdempotent() {
            Account account = phoneAccount();
            account.bindWechat("oid-1", "uid-1");
            account.bindWechat("oid-1", "uid-1");
            assertEquals("oid-1", account.getWechatBinding().getOpenid());
        }

        @Test
        @DisplayName("小程序绑定不应清掉已有的网页渠道 openid")
        void miniProgramShouldPreserveWebOpenid() {
            Account account = phoneAccount();
            account.bindWechatWeb("web-1", "uid-1");
            account.bindWechat("oid-1", "uid-1");
            assertEquals("web-1", account.getWechatBinding().getWebOpenid());
            assertEquals("oid-1", account.getWechatBinding().getOpenid());
        }

        @Test
        @DisplayName("网页渠道已绑不同 webOpenid 应抛 WECHAT_ALREADY_BOUND")
        void webConflict() {
            Account account = phoneAccount();
            account.bindWechatWeb("web-1", "uid-1");
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.bindWechatWeb("web-2", "uid-1"));
            assertEquals(AuthErrorCode.WECHAT_ALREADY_BOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("H5 渠道已绑不同 mpOpenid 应抛 WECHAT_ALREADY_BOUND")
        void h5Conflict() {
            Account account = phoneAccount();
            account.bindWechatH5("mp-1", "uid-1");
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.bindWechatH5("mp-2", "uid-1"));
            assertEquals(AuthErrorCode.WECHAT_ALREADY_BOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("unionid 不同视为不同微信主体，应抛 WECHAT_ALREADY_BOUND")
        void unionidConflict() {
            Account account = phoneAccount();
            account.bindWechat("oid-1", "uid-1");
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.bindWechatWeb("web-1", "uid-OTHER"));
            assertEquals(AuthErrorCode.WECHAT_ALREADY_BOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("同一 unionid 跨渠道补绑应成功")
        void crossChannelSameUnionid() {
            Account account = phoneAccount();
            account.bindWechat("oid-1", "uid-1");
            account.bindWechatWeb("web-1", "uid-1");
            account.bindWechatH5("mp-1", "uid-1");
            assertEquals("oid-1", account.getWechatBinding().getOpenid());
            assertEquals("web-1", account.getWechatBinding().getWebOpenid());
            assertEquals("mp-1", account.getWechatBinding().getMpOpenid());
            assertEquals("uid-1", account.getWechatBinding().getUnionid());
        }
    }

    @Nested
    @DisplayName("bindTaobao 现有语义回归")
    class BindTaobao {

        @Test
        @DisplayName("幂等重绑相同 openUid 应成功")
        void idempotent() {
            Account account = phoneAccount();
            account.bindTaobao("tb-1");
            account.bindTaobao("tb-1");
            assertEquals("tb-1", account.getTaobaoBinding().getOpenUid());
        }

        @Test
        @DisplayName("已绑不同 openUid 应抛 TAOBAO_ALREADY_BOUND")
        void conflict() {
            Account account = phoneAccount();
            account.bindTaobao("tb-1");
            assertThrows(DomainException.class, () -> account.bindTaobao("tb-2"));
        }
    }

    @Test
    @DisplayName("createFromPhone 账号可同时持有淘宝+Apple+微信绑定")
    void multiProviderCoexist() {
        Account account = phoneAccount();
        account.bindTaobao("tb-1");
        account.bindApple("sub-1", "cipher-1");
        account.bindWechat("oid-1", "uid-1");
        assertTrue(account.getTaobaoBinding() != null
                && account.getAppleBinding() != null
                && account.getWechatBinding() != null);
    }
}

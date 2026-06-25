package com.eagle.auth.core.domain.model;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "{bcrypt}xxx";
    private static final String PHONE = "13800138000";
    private static final String OPENID = "wx_openid_abcdef0123456789longer";
    private static final String UNIONID = "wx_unionid_xyz";
    private static final ProfileHints HINTS = new ProfileHints("Alice", "https://avatar.example/a.png", "alice@example.com");

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("ArgumentsAre有效时应创建账号")
        void shouldCreateAccountWhenArgumentsAreValid() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);

            assertEquals(USERNAME, account.getUsername());
            assertEquals(PASSWORD, account.getPassword());
            assertEquals(PHONE, account.getPhone());
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertSame(HINTS, account.getProfileHints());
            assertNull(account.getWechatBinding());
        }

        @Test
        @DisplayName("用户名缺失时应抛出")
        void shouldThrowWhenUsernameMissing() {
            AppException ex1 = assertThrows(DomainException.class,
                    () -> Account.create(null, PASSWORD, PHONE, HINTS));
            assertEquals(AuthErrorCode.ACCOUNT_USERNAME_REQUIRED, ex1.getErrorCode());

            AppException ex2 = assertThrows(DomainException.class,
                    () -> Account.create("  ", PASSWORD, PHONE, HINTS));
            assertEquals(AuthErrorCode.ACCOUNT_USERNAME_REQUIRED, ex2.getErrorCode());
        }

        @Test
        @DisplayName("密码缺失时应抛出")
        void shouldThrowWhenPasswordMissing() {
            AppException ex = assertThrows(DomainException.class,
                    () -> Account.create(USERNAME, null, PHONE, HINTS));
            assertEquals(AuthErrorCode.PASSWORD_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createFromPhone")
    class CreateFromPhone {

        @Test
        @DisplayName("应创建手机号账号")
        void shouldCreatePhoneAccount() {
            Account account = Account.createFromPhone(PHONE);

            assertEquals(PHONE, account.getUsername());
            assertEquals(PHONE, account.getPhone());
            assertEquals(Account.DISABLED_PASSWORD, account.getPassword());
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertSame(ProfileHints.EMPTY, account.getProfileHints());
        }

        @Test
        @DisplayName("手机号空白时应抛出")
        void shouldThrowWhenPhoneBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> Account.createFromPhone(""));
            assertEquals(AuthErrorCode.PHONE_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createFromWechat")
    class CreateFromWechat {

        @Test
        @DisplayName("应创建MiniProgram账号")
        void shouldCreateMiniProgramAccount() {
            Account account = Account.createFromWechat(OPENID, UNIONID);

            // SHA-256 短哈希前 16 hex 字符，避免单纯截 openid 前 16 字符碰撞
            assertEquals(16 + "wx_".length(), account.getUsername().length());
            assertEquals(Account.DISABLED_PASSWORD, account.getPassword());
            assertNotNull(account.getWechatBinding());
            assertEquals(OPENID, account.getWechatBinding().getOpenid());
            assertEquals(UNIONID, account.getWechatBinding().getUnionid());
        }

        @Test
        @DisplayName("openid空白时应抛出")
        void shouldThrowWhenOpenidBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> Account.createFromWechat(" ", UNIONID));
            assertEquals(AuthErrorCode.OPENID_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createFromWechatWeb")
    class CreateFromWechatWeb {

        @Test
        @DisplayName("应创建Web账号")
        void shouldCreateWebAccount() {
            Account account = Account.createFromWechatWeb(OPENID, UNIONID, "NickName", "https://a.png");
            assertEquals(16 + "wxweb_".length(), account.getUsername().length());
            assertTrue(account.getUsername().startsWith("wxweb_"));
            assertEquals(OPENID, account.getWechatBinding().getWebOpenid());
            assertEquals("NickName", account.getProfileHints().nickname());
            assertEquals("https://a.png", account.getProfileHints().avatar());
        }

        @Test
        @DisplayName("空白时应抛出")
        void shouldThrowWhenBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> Account.createFromWechatWeb(null, UNIONID, null, null));
            assertEquals(AuthErrorCode.WEB_OPENID_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createFromWechatH5")
    class CreateFromWechatH5 {

        @Test
        @DisplayName("应创建小时5账号")
        void shouldCreateH5Account() {
            Account account = Account.createFromWechatH5(OPENID, UNIONID, null, null);
            assertEquals(16 + "wxmp_".length(), account.getUsername().length());
            assertTrue(account.getUsername().startsWith("wxmp_"));
            assertEquals(OPENID, account.getWechatBinding().getMpOpenid());
        }

        @Test
        @DisplayName("空白时应抛出")
        void shouldThrowWhenBlank() {
            AppException ex = assertThrows(DomainException.class,
                    () -> Account.createFromWechatH5("", UNIONID, null, null));
            assertEquals(AuthErrorCode.MP_OPENID_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("应更新密码")
        void shouldUpdatePassword() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.changePassword("{bcrypt}newhash");
            assertEquals("{bcrypt}newhash", account.getPassword());
        }

        @Test
        @DisplayName("New密码空白时应抛出")
        void shouldThrowWhenNewPasswordBlank() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            AppException ex = assertThrows(DomainException.class,
                    () -> account.changePassword(" "));
            assertEquals(AuthErrorCode.NEW_PASSWORD_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("bindPhone")
    class BindPhone {

        @Test
        @DisplayName("应设置手机号")
        void shouldSetPhone() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            account.bindPhone(PHONE);
            assertEquals(PHONE, account.getPhone());
        }

        @Test
        @DisplayName("已经Bound时应抛出")
        void shouldThrowWhenAlreadyBound() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            AppException ex = assertThrows(DomainException.class,
                    () -> account.bindPhone("13900000000"));
            assertEquals(AuthErrorCode.ACCOUNT_PHONE_ALREADY_SET, ex.getErrorCode());
        }

        @Test
        @DisplayName("空白时应抛出")
        void shouldThrowWhenBlank() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            AppException ex = assertThrows(DomainException.class, () -> account.bindPhone(""));
            assertEquals(AuthErrorCode.PHONE_REQUIRED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("freeze / unfreeze")
    class FreezeUnfreeze {

        @Test
        @DisplayName("应冻结Active")
        void shouldFreezeActive() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.freezeByAdmin(null, "tester",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.OTHER, null, "test");
            assertEquals(AccountStatus.FROZEN, account.getStatus());
        }

        @Test
        @DisplayName("已经已冻结时应抛出")
        void shouldThrowWhenAlreadyFrozen() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.freezeByAdmin(null, "tester",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.OTHER, null, null);
            AppException ex = assertThrows(DomainException.class, () -> account.freezeByAdmin(
                    null, "tester",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.OTHER, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_FROZEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("应解冻已冻结")
        void shouldUnfreezeFrozen() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.freezeByAdmin(null, "tester",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.OTHER, null, null);
            account.unfreeze(null, "tester");
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
        }

        @Test
        @DisplayName("不已冻结时应抛出")
        void shouldThrowWhenNotFrozen() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            AppException ex = assertThrows(DomainException.class, () -> account.unfreeze(null, "tester"));
            assertEquals(AuthErrorCode.ACCOUNT_NOT_FROZEN, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("bindWechat variants")
    class BindWechatVariants {

        @Test
        @DisplayName("绑定微信创建Binding")
        void bindWechatCreatesBinding() {
            Account account = Account.createFromPhone(PHONE);
            account.bindWechat(OPENID, UNIONID);
            assertEquals(OPENID, account.getWechatBinding().getOpenid());
        }

        @Test
        @DisplayName("绑定微信抛出当空白")
        void bindWechatThrowsWhenBlank() {
            Account account = Account.createFromPhone(PHONE);
            AppException ex = assertThrows(DomainException.class,
                    () -> account.bindWechat(null, UNIONID));
            assertEquals(AuthErrorCode.OPENID_REQUIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("绑定微信WebMerges到已有Binding")
        void bindWechatWebMergesIntoExistingBinding() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            account.bindWechatWeb("web_openid_123456789012345", null);
            assertEquals(OPENID, account.getWechatBinding().getOpenid());
            assertEquals("web_openid_123456789012345", account.getWechatBinding().getWebOpenid());
        }

        @Test
        @DisplayName("绑定微信小时5Merges到已有Binding")
        void bindWechatH5MergesIntoExistingBinding() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            account.bindWechatH5("mp_openid_123456789012345", "new_unionid");
            assertEquals("mp_openid_123456789012345", account.getWechatBinding().getMpOpenid());
            assertEquals("new_unionid", account.getWechatBinding().getUnionid());
        }
    }

    @Nested
    @DisplayName("changePhone")
    class ChangePhone {

        @Test
        @DisplayName("替换为新号时应更新 phone")
        void shouldReplacePhone() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);

            account.changePhone("13900139000");

            assertEquals("13900139000", account.getPhone());
        }

        @Test
        @DisplayName("空号时应抛 PHONE_REQUIRED")
        void shouldThrowWhenBlank() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);

            AppException ex = assertThrows(DomainException.class, () -> account.changePhone("  "));
            assertEquals(AuthErrorCode.PHONE_REQUIRED, ex.getErrorCode());
        }
    }
}

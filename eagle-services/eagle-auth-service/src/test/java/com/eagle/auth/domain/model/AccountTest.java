package com.eagle.auth.domain.model;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.auth.domain.AuthErrorCode;
import com.eagle.auth.domain.model.enums.AccountStatus;
import com.eagle.auth.domain.model.valueobject.ProfileHints;
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
        @DisplayName("should create account with all fields when arguments are valid")
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
        @DisplayName("should throw when username is null or blank")
        void shouldThrowWhenUsernameMissing() {
            AppException ex1 = assertThrows(DomainException.class,
                    () -> Account.create(null, PASSWORD, PHONE, HINTS));
            assertEquals(AuthErrorCode.ACCOUNT_USERNAME_REQUIRED, ex1.getErrorCode());

            AppException ex2 = assertThrows(DomainException.class,
                    () -> Account.create("  ", PASSWORD, PHONE, HINTS));
            assertEquals(AuthErrorCode.ACCOUNT_USERNAME_REQUIRED, ex2.getErrorCode());
        }

        @Test
        @DisplayName("should throw when password is null or blank")
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
        @DisplayName("should create phone-only account with empty password")
        void shouldCreatePhoneAccount() {
            Account account = Account.createFromPhone(PHONE);

            assertEquals(PHONE, account.getUsername());
            assertEquals(PHONE, account.getPhone());
            assertEquals(Account.DISABLED_PASSWORD, account.getPassword());
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertSame(ProfileHints.EMPTY, account.getProfileHints());
        }

        @Test
        @DisplayName("should throw when phone is blank")
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
        @DisplayName("should derive username from sha-256 of openid (no first-16-char collision)")
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
        @DisplayName("should throw when openid is blank")
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
        @DisplayName("should set web binding and merge profile hints")
        void shouldCreateWebAccount() {
            Account account = Account.createFromWechatWeb(OPENID, UNIONID, "NickName", "https://a.png");
            assertEquals(16 + "wxweb_".length(), account.getUsername().length());
            assertTrue(account.getUsername().startsWith("wxweb_"));
            assertEquals(OPENID, account.getWechatBinding().getWebOpenid());
            assertEquals("NickName", account.getProfileHints().nickname());
            assertEquals("https://a.png", account.getProfileHints().avatar());
        }

        @Test
        @DisplayName("should throw when web openid blank")
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
        @DisplayName("should set mp binding")
        void shouldCreateH5Account() {
            Account account = Account.createFromWechatH5(OPENID, UNIONID, null, null);
            assertEquals(16 + "wxmp_".length(), account.getUsername().length());
            assertTrue(account.getUsername().startsWith("wxmp_"));
            assertEquals(OPENID, account.getWechatBinding().getMpOpenid());
        }

        @Test
        @DisplayName("should throw when mp openid blank")
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
        @DisplayName("should update password")
        void shouldUpdatePassword() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.changePassword("{bcrypt}newhash");
            assertEquals("{bcrypt}newhash", account.getPassword());
        }

        @Test
        @DisplayName("should throw when new password is blank")
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
        @DisplayName("should set phone when none present")
        void shouldSetPhone() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            account.bindPhone(PHONE);
            assertEquals(PHONE, account.getPhone());
        }

        @Test
        @DisplayName("should throw when account already bound to a phone")
        void shouldThrowWhenAlreadyBound() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            AppException ex = assertThrows(DomainException.class,
                    () -> account.bindPhone("13900000000"));
            assertEquals(AuthErrorCode.ACCOUNT_PHONE_ALREADY_SET, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when input phone is blank")
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
        @DisplayName("should freeze an active account via freezeByAdmin()")
        void shouldFreezeActive() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.freezeByAdmin(null, "tester",
                    com.eagle.auth.domain.model.enums.FreezeReason.OTHER, null, "test");
            assertEquals(AccountStatus.FROZEN, account.getStatus());
        }

        @Test
        @DisplayName("should throw ACCOUNT_FROZEN when freezing an already-frozen account")
        void shouldThrowWhenAlreadyFrozen() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.freezeByAdmin(null, "tester",
                    com.eagle.auth.domain.model.enums.FreezeReason.OTHER, null, null);
            AppException ex = assertThrows(DomainException.class, () -> account.freezeByAdmin(
                    null, "tester",
                    com.eagle.auth.domain.model.enums.FreezeReason.OTHER, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_FROZEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("should unfreeze a frozen account via unfreeze()")
        void shouldUnfreezeFrozen() {
            Account account = Account.create(USERNAME, PASSWORD, PHONE, HINTS);
            account.freezeByAdmin(null, "tester",
                    com.eagle.auth.domain.model.enums.FreezeReason.OTHER, null, null);
            account.unfreeze(null, "tester");
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
        }

        @Test
        @DisplayName("should throw ACCOUNT_NOT_FROZEN when unfreezing an active account")
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
        @DisplayName("bindWechat should create binding when none exists")
        void bindWechatCreatesBinding() {
            Account account = Account.createFromPhone(PHONE);
            account.bindWechat(OPENID, UNIONID);
            assertEquals(OPENID, account.getWechatBinding().getOpenid());
        }

        @Test
        @DisplayName("bindWechat should throw when openid blank")
        void bindWechatThrowsWhenBlank() {
            Account account = Account.createFromPhone(PHONE);
            AppException ex = assertThrows(DomainException.class,
                    () -> account.bindWechat(null, UNIONID));
            assertEquals(AuthErrorCode.OPENID_REQUIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("bindWechatWeb should set webOpenid on existing binding")
        void bindWechatWebMergesIntoExistingBinding() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            account.bindWechatWeb("web_openid_123456789012345", null);
            assertEquals(OPENID, account.getWechatBinding().getOpenid());
            assertEquals("web_openid_123456789012345", account.getWechatBinding().getWebOpenid());
        }

        @Test
        @DisplayName("bindWechatH5 should set mpOpenid on existing binding")
        void bindWechatH5MergesIntoExistingBinding() {
            Account account = Account.createFromWechat(OPENID, UNIONID);
            account.bindWechatH5("mp_openid_123456789012345", "new_unionid");
            assertEquals("mp_openid_123456789012345", account.getWechatBinding().getMpOpenid());
            assertEquals("new_unionid", account.getWechatBinding().getUnionid());
        }
    }
}

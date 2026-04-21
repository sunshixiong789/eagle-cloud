package com.eagle.auth.domain.model;

import com.eagle.auth.domain.model.valueobject.ProfileHints;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Account 聚合根单元测试
 *
 * @author sunshixiong
 */
@DisplayName("Account 聚合根")
class AccountTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create account when all fields are valid")
        void shouldCreateAccountWhenAllFieldsAreValid() {
            Account account = Account.create("admin", "encrypted_pwd", "13800000000", ProfileHints.EMPTY);

            assertEquals("admin", account.getUsername());
            assertEquals("encrypted_pwd", account.getPassword());
            assertEquals("13800000000", account.getPhone());
            assertFalse(account.getLocked());
            assertNull(account.getWechatBinding());
        }

        @Test
        @DisplayName("should create account when phone is null")
        void shouldCreateAccountWhenPhoneIsNull() {
            Account account = Account.create("admin", "encrypted_pwd", null, ProfileHints.EMPTY);

            assertEquals("admin", account.getUsername());
            assertNull(account.getPhone());
        }

        @Test
        @DisplayName("should throw DomainException when username is null")
        void shouldThrowWhenUsernameIsNull() {
            assertThrows(DomainException.class,
                () -> Account.create(null, "pwd", null, ProfileHints.EMPTY));
        }

        @Test
        @DisplayName("should throw DomainException when username is blank")
        void shouldThrowWhenUsernameIsBlank() {
            assertThrows(DomainException.class,
                () -> Account.create("  ", "pwd", null, ProfileHints.EMPTY));
        }

        @Test
        @DisplayName("should throw DomainException when password is null")
        void shouldThrowWhenPasswordIsNull() {
            assertThrows(DomainException.class,
                () -> Account.create("admin", null, null, ProfileHints.EMPTY));
        }

        @Test
        @DisplayName("should throw DomainException when password is blank")
        void shouldThrowWhenPasswordIsBlank() {
            assertThrows(DomainException.class,
                () -> Account.create("admin", "  ", null, ProfileHints.EMPTY));
        }
    }

    @Nested
    @DisplayName("createFromWechat")
    class CreateFromWechat {

        @Test
        @DisplayName("should create account from wechat openid")
        void shouldCreateAccountFromWechatOpenid() {
            Account account = Account.createFromWechat(
                "oXyz1234567890abcdef", "unionid_abc");

            assertEquals("wx_oXyz1234567890ab", account.getUsername());
            assertEquals("", account.getPassword());
            assertFalse(account.getLocked());
            assertNotNull(account.getWechatBinding());
            assertEquals("oXyz1234567890abcdef",
                account.getWechatBinding().getOpenid());
            assertEquals("unionid_abc",
                account.getWechatBinding().getUnionid());
        }

        @Test
        @DisplayName("should throw DomainException when openid is null")
        void shouldThrowWhenOpenidIsNull() {
            assertThrows(DomainException.class,
                () -> Account.createFromWechat(null, "unionid"));
        }

        @Test
        @DisplayName("should throw DomainException when openid is blank")
        void shouldThrowWhenOpenidIsBlank() {
            assertThrows(DomainException.class,
                () -> Account.createFromWechat("  ", "unionid"));
        }
    }

    @Nested
    @DisplayName("createFromPhone")
    class CreateFromPhone {

        @Test
        @DisplayName("should create account from phone")
        void shouldCreateAccountFromPhone() {
            Account account = Account.createFromPhone("13800000000");

            assertEquals("13800000000", account.getUsername());
            assertEquals("", account.getPassword());
            assertEquals("13800000000", account.getPhone());
            assertFalse(account.getLocked());
        }

        @Test
        @DisplayName("should throw DomainException when phone is blank")
        void shouldThrowWhenPhoneIsBlank() {
            assertThrows(DomainException.class,
                () -> Account.createFromPhone(""));
        }

        @Test
        @DisplayName("should throw DomainException when phone is null")
        void shouldThrowWhenPhoneIsNull() {
            assertThrows(DomainException.class,
                () -> Account.createFromPhone(null));
        }
    }

    @Nested
    @DisplayName("createFromWechatWeb")
    class CreateFromWechatWeb {

        @Test
        @DisplayName("should create account from wechat web openid")
        void shouldCreateAccountFromWechatWebOpenid() {
            Account account = Account.createFromWechatWeb(
                "web_openid_1234567890", "unionid_web", "Tom", "http://img.url");

            assertEquals("wxweb_web_openid_12345", account.getUsername());
            assertEquals("", account.getPassword());
            assertFalse(account.getLocked());
            assertNotNull(account.getWechatBinding());
            assertEquals("web_openid_1234567890",
                account.getWechatBinding().getWebOpenid());
        }

        @Test
        @DisplayName("should throw DomainException when webOpenid is null")
        void shouldThrowWhenWebOpenidIsNull() {
            assertThrows(DomainException.class,
                () -> Account.createFromWechatWeb(null, "u", "n", "a"));
        }

        @Test
        @DisplayName("should throw DomainException when webOpenid is blank")
        void shouldThrowWhenWebOpenidIsBlank() {
            assertThrows(DomainException.class,
                () -> Account.createFromWechatWeb("", "u", "n", "a"));
        }
    }

    @Nested
    @DisplayName("createFromWechatH5")
    class CreateFromWechatH5 {

        @Test
        @DisplayName("should create account from wechat H5 openid")
        void shouldCreateAccountFromWechatH5Openid() {
            Account account = Account.createFromWechatH5(
                "mp_openid_1234567890", "unionid_mp", "Jerry", "http://img.url");

            assertEquals("wxmp_mp_openid_123456", account.getUsername());
            assertEquals("", account.getPassword());
            assertFalse(account.getLocked());
            assertNotNull(account.getWechatBinding());
            assertEquals("mp_openid_1234567890",
                account.getWechatBinding().getMpOpenid());
        }

        @Test
        @DisplayName("should throw DomainException when mpOpenid is null")
        void shouldThrowWhenMpOpenidIsNull() {
            assertThrows(DomainException.class,
                () -> Account.createFromWechatH5(null, "u", "n", "a"));
        }

        @Test
        @DisplayName("should throw DomainException when mpOpenid is blank")
        void shouldThrowWhenMpOpenidIsBlank() {
            assertThrows(DomainException.class,
                () -> Account.createFromWechatH5("  ", "u", "n", "a"));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password when new password is valid")
        void shouldChangePasswordWhenValid() {
            Account account = Account.create("admin", "old_pwd", null, ProfileHints.EMPTY);

            account.changePassword("new_pwd");

            assertEquals("new_pwd", account.getPassword());
        }

        @Test
        @DisplayName("should throw DomainException when new password is blank")
        void shouldThrowWhenNewPasswordIsBlank() {
            Account account = Account.create("admin", "old_pwd", null, ProfileHints.EMPTY);

            assertThrows(DomainException.class,
                () -> account.changePassword(""));
        }

        @Test
        @DisplayName("should throw DomainException when new password is null")
        void shouldThrowWhenNewPasswordIsNull() {
            Account account = Account.create("admin", "old_pwd", null, ProfileHints.EMPTY);

            assertThrows(DomainException.class,
                () -> account.changePassword(null));
        }
    }

    @Nested
    @DisplayName("lock")
    class Lock {

        @Test
        @DisplayName("should lock account when not locked")
        void shouldLockAccountWhenNotLocked() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            account.lock();

            assertTrue(account.getLocked());
        }

        @Test
        @DisplayName("should throw DomainException when already locked")
        void shouldThrowWhenAlreadyLocked() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);
            account.lock();

            assertThrows(DomainException.class, account::lock);
        }
    }

    @Nested
    @DisplayName("unlock")
    class Unlock {

        @Test
        @DisplayName("should unlock account when locked")
        void shouldUnlockAccountWhenLocked() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);
            account.lock();

            account.unlock();

            assertFalse(account.getLocked());
        }

        @Test
        @DisplayName("should throw DomainException when not locked")
        void shouldThrowWhenNotLocked() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            assertThrows(DomainException.class, account::unlock);
        }
    }

    @Nested
    @DisplayName("bindWechatWeb")
    class BindWechatWeb {

        @Test
        @DisplayName("should bind wechat web when no existing binding")
        void shouldBindWechatWebWhenNoExistingBinding() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            account.bindWechatWeb("web_openid_123", "unionid_123");

            assertNotNull(account.getWechatBinding());
            assertEquals("web_openid_123",
                account.getWechatBinding().getWebOpenid());
            assertEquals("unionid_123",
                account.getWechatBinding().getUnionid());
        }

        @Test
        @DisplayName("should update web openid when existing binding present")
        void shouldUpdateWebOpenidWhenExistingBindingPresent() {
            Account account = Account.createFromWechat("openid_abc", "unionid_old");

            account.bindWechatWeb("web_openid_new", "unionid_new");

            assertEquals("openid_abc",
                account.getWechatBinding().getOpenid());
            assertEquals("web_openid_new",
                account.getWechatBinding().getWebOpenid());
            assertEquals("unionid_new",
                account.getWechatBinding().getUnionid());
        }

        @Test
        @DisplayName("should throw DomainException when webOpenid is blank")
        void shouldThrowWhenWebOpenidIsBlank() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            assertThrows(DomainException.class,
                () -> account.bindWechatWeb("", "unionid"));
        }
    }

    @Nested
    @DisplayName("bindWechatH5")
    class BindWechatH5 {

        @Test
        @DisplayName("should bind wechat H5 when no existing binding")
        void shouldBindWechatH5WhenNoExistingBinding() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            account.bindWechatH5("mp_openid_123", "unionid_123");

            assertNotNull(account.getWechatBinding());
            assertEquals("mp_openid_123",
                account.getWechatBinding().getMpOpenid());
        }

        @Test
        @DisplayName("should update mp openid when existing binding present")
        void shouldUpdateMpOpenidWhenExistingBindingPresent() {
            Account account = Account.createFromWechat("openid_abc", "unionid_old");

            account.bindWechatH5("mp_openid_new", "unionid_new");

            assertEquals("openid_abc",
                account.getWechatBinding().getOpenid());
            assertEquals("mp_openid_new",
                account.getWechatBinding().getMpOpenid());
            assertEquals("unionid_new",
                account.getWechatBinding().getUnionid());
        }

        @Test
        @DisplayName("should throw DomainException when mpOpenid is blank")
        void shouldThrowWhenMpOpenidIsBlank() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            assertThrows(DomainException.class,
                () -> account.bindWechatH5("  ", "unionid"));
        }
    }

    @Nested
    @DisplayName("bindWechat")
    class BindWechat {

        @Test
        @DisplayName("should bind wechat mini program")
        void shouldBindWechatMiniProgram() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            account.bindWechat("openid_mini", "unionid_mini");

            assertNotNull(account.getWechatBinding());
            assertEquals("openid_mini",
                account.getWechatBinding().getOpenid());
            assertEquals("unionid_mini",
                account.getWechatBinding().getUnionid());
        }

        @Test
        @DisplayName("should throw DomainException when openid is blank")
        void shouldThrowWhenOpenidIsBlank() {
            Account account = Account.create("admin", "pwd", null, ProfileHints.EMPTY);

            assertThrows(DomainException.class,
                () -> account.bindWechat("", "unionid"));
        }
    }
}

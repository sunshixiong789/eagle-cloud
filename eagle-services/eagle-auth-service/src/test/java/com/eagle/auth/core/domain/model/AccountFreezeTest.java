package com.eagle.auth.core.domain.model;

import com.eagle.common.exception.DomainException;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.model.enums.FreezeReason;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountFreezeTest {

    private static final Long OPERATOR_ID = 99L;
    private static final String OPERATOR_NAME = "admin";

    private Account newActiveAccount() {
        return Account.create("alice", "{bcrypt}x", "13800138000",
                new ProfileHints("Alice", null, null));
    }

    @Nested
    @DisplayName("freezeByAdmin")
    class Freeze {
        @Test
        @DisplayName("应冻结Active账号")
        void shouldFreezeActiveAccount() {
            Account account = newActiveAccount();
            LocalDateTime until = LocalDateTime.now().plusHours(1);

            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, until, "test");

            assertEquals(AccountStatus.FROZEN, account.getStatus());
            assertNotNull(account.getFreeze());
            assertEquals(FreezeReason.ADMIN, account.getFreeze().getReason());
            assertEquals(until, account.getFreeze().getFreezeUntil());
            assertEquals(OPERATOR_ID, account.getFreeze().getOperatorId());
        }

        @Test
        @DisplayName("已经已冻结时应拒绝")
        void shouldRejectWhenAlreadyFrozen() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null);
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_FROZEN.getCode(), ex.getErrorCode().getCode());
        }

        @Test
        @DisplayName("应拒绝Past冻结Until")
        void shouldRejectPastFreezeUntil() {
            Account account = newActiveAccount();
            LocalDateTime past = LocalDateTime.now().minusMinutes(1);
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, past, null));
            assertEquals(AuthErrorCode.ACCOUNT_FREEZE_UNTIL_INVALID.getCode(), ex.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("unfreeze")
    class Unfreeze {
        @Test
        @DisplayName("应解冻")
        void shouldUnfreeze() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null);

            account.unfreeze(OPERATOR_ID, OPERATOR_NAME);

            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertNull(account.getFreeze());
        }

        @Test
        @DisplayName("不已冻结时应拒绝")
        void shouldRejectWhenNotFrozen() {
            Account account = newActiveAccount();
            DomainException ex = assertThrows(DomainException.class,
                    () -> account.unfreeze(OPERATOR_ID, OPERATOR_NAME));
            assertEquals(AuthErrorCode.ACCOUNT_NOT_FROZEN.getCode(), ex.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("tryAutoUnfreezeIfExpired")
    class AutoUnfreeze {
        @Test
        @DisplayName("Expired时应Auto解冻")
        void shouldAutoUnfreezeWhenExpired() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN,
                    LocalDateTime.now().plusSeconds(1), null);
            boolean unfrozen = account.tryAutoUnfreezeIfExpired(
                    LocalDateTime.now().plusMinutes(1));
            assertEquals(true, unfrozen);
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
        }

        @Test
        @DisplayName("永久有效时不应解冻")
        void shouldNotUnfreezeWhenPermanent() {
            Account account = newActiveAccount();
            account.freezeByAdmin(OPERATOR_ID, OPERATOR_NAME, FreezeReason.ADMIN, null, null);
            boolean unfrozen = account.tryAutoUnfreezeIfExpired(LocalDateTime.now().plusYears(10));
            assertEquals(false, unfrozen);
            assertEquals(AccountStatus.FROZEN, account.getStatus());
        }
    }
}

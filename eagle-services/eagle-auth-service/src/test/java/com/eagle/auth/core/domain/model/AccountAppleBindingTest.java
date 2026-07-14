package com.eagle.auth.core.domain.model;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountAppleBindingTest {

    @Test
    void createsAccountFromVerifiedAppleSubject() {
        Account account = Account.createFromApple("apple-subject-1", "relay@privaterelay.appleid.com", "小明");

        assertTrue(account.getUsername().startsWith("apple_"));
        assertEquals(Account.DISABLED_PASSWORD, account.getPassword());
        assertNotNull(account.getAppleBinding());
        assertEquals("apple-subject-1", account.getAppleBinding().getSubject());
        assertEquals("小明", account.getProfileHints().nickname());
    }

    @Test
    void rejectsMissingAppleSubject() {
        DomainException error = assertThrows(
                DomainException.class, () -> Account.createFromApple(" ", null, null));

        assertEquals(AuthErrorCode.APPLE_SUBJECT_REQUIRED, error.getErrorCode());
    }
}

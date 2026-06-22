package com.eagle.auth.core.domain.model;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTaobaoBindingTest {

    @Test
    @DisplayName("bindTaobao 应记录 openUid")
    void bindTaobaoStoresOpenUid() {
        Account account = Account.createFromPhone("13800138000");

        account.bindTaobao("tb-uid-1");

        assertEquals("tb-uid-1", account.getTaobaoBinding().getOpenUid());
    }

    @Test
    @DisplayName("openUid 为空应抛 TAOBAO_AUTH_REQUIRED")
    void bindTaobaoRejectsBlankOpenUid() {
        Account account = Account.createFromPhone("13800138000");

        AppException ex = assertThrows(AppException.class, () -> account.bindTaobao(" "));
        assertEquals(AuthErrorCode.TAOBAO_AUTH_REQUIRED.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("重复绑定不同 openUid 应抛 TAOBAO_ALREADY_BOUND")
    void bindTaobaoRejectsConflictingOpenUid() {
        Account account = Account.createFromPhone("13800138000");
        account.bindTaobao("tb-uid-1");

        AppException ex = assertThrows(AppException.class, () -> account.bindTaobao("tb-uid-2"));
        assertEquals(AuthErrorCode.TAOBAO_ALREADY_BOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("重复绑定相同 openUid 幂等通过")
    void bindTaobaoIsIdempotentForSameOpenUid() {
        Account account = Account.createFromPhone("13800138000");
        account.bindTaobao("tb-uid-1");

        account.bindTaobao("tb-uid-1");

        assertEquals("tb-uid-1", account.getTaobaoBinding().getOpenUid());
    }
}

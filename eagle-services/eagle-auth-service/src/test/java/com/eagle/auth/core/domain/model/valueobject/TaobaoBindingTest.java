package com.eagle.auth.core.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaobaoBindingTest {

    @Test
    @DisplayName("create 应保存 openUid 并填充绑定时间")
    void createStoresOpenUidAndBindTime() {
        TaobaoBinding binding = TaobaoBinding.create("tb-open-uid-1");

        assertEquals("tb-open-uid-1", binding.getOpenUid());
        assertNotNull(binding.getBindTime());
    }
}

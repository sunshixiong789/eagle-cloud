package com.eagle.auth.core.infrastructure.event;

import com.eagle.auth.core.domain.event.AccountFrozenEvent;
import com.eagle.auth.core.domain.model.enums.FreezeReason;
import com.eagle.auth.core.domain.port.OnlineUserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityEventHandlerTest {

    @Mock
    OnlineUserPort onlineUserPort;
    @InjectMocks
    AccountSecurityEventHandler handler;

    @Test
    @DisplayName("应强制登出所有 JTI")
    void shouldForceLogoutAllJtis() {
        when(onlineUserPort.listJtisByAccount(100L))
                .thenReturn(List.of("jti-1", "jti-2", "jti-3"));

        handler.onAccountFrozen(new AccountFrozenEvent(
                100L, "alice", FreezeReason.ADMIN, null, 99L));

        verify(onlineUserPort).forceLogout("jti-1");
        verify(onlineUserPort).forceLogout("jti-2");
        verify(onlineUserPort).forceLogout("jti-3");
        verify(onlineUserPort, times(3)).forceLogout(anyString());
    }
}

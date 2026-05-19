package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.AccountFrozenEvent;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 账号安全事件处理器：冻结 → 强制下线该账号所有在线 token
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSecurityEventHandler {

    private final OnlineUserPort onlineUserPort;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccountFrozen(AccountFrozenEvent event) {
        List<String> jtis = onlineUserPort.listJtisByAccount(event.accountId());
        for (String jti : jtis) {
            onlineUserPort.forceLogout(jti);
        }
        log.info("frozen account force-logout: accountId={}, jtiCount={}",
                event.accountId(), jtis.size());
    }
}

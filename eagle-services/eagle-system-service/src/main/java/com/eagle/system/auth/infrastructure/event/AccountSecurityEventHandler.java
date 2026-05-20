package com.eagle.system.auth.infrastructure.event;

import com.eagle.system.auth.domain.event.AccountDeletedEvent;
import com.eagle.system.auth.domain.event.AccountFrozenEvent;
import com.eagle.system.auth.domain.event.AccountUnfrozenEvent;
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
 * 账号安全事件处理器。
 *
 * <p>覆盖以下安全审计与副作用：
 * <ul>
 *   <li>冻结 → 强制下线该账号所有在线 token + 审计日志</li>
 *   <li>解冻 → 审计日志</li>
 *   <li>删除 → 强制下线 + 审计日志</li>
 * </ul>
 *
 * <p>审计当前以 {@code log.info} 结构化输出（由 Logback JSON encoder 收敛到 ELK），
 * 后续接入 {@code eagle-audit-log-starter} 可替换为 {@code @AuditLog} 切面。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSecurityEventHandler {

    private static final String AUDIT_MARKER = "AUDIT";

    private final OnlineUserPort onlineUserPort;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccountFrozen(AccountFrozenEvent event) {
        List<String> jtis = onlineUserPort.listJtisByAccount(event.accountId());
        for (String jti : jtis) {
            onlineUserPort.forceLogout(jti);
        }
        log.info("[{}] action=ACCOUNT_FROZEN, accountId={}, username={}, reason={}, operatorId={}, jtiCount={}",
                AUDIT_MARKER, event.accountId(), event.username(),
                event.reason(), event.operatorId(), jtis.size());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountUnfrozen(AccountUnfrozenEvent event) {
        log.info("[{}] action=ACCOUNT_UNFROZEN, accountId={}, username={}, source={}, operatorId={}",
                AUDIT_MARKER, event.accountId(), event.username(),
                event.source(), event.operatorId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccountDeleted(AccountDeletedEvent event) {
        List<String> jtis = onlineUserPort.listJtisByAccount(event.accountId());
        for (String jti : jtis) {
            onlineUserPort.forceLogout(jti);
        }
        log.info("[{}] action=ACCOUNT_DELETED, accountId={}, jtiCount={}",
                AUDIT_MARKER, event.accountId(), jtis.size());
    }
}

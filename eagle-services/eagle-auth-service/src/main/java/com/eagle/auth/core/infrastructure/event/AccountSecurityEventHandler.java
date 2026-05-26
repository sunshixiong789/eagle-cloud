package com.eagle.auth.core.infrastructure.event;

import com.eagle.auth.core.domain.event.AccountDeletedEvent;
import com.eagle.auth.core.domain.event.AccountFrozenEvent;
import com.eagle.auth.core.domain.event.AccountUnfrozenEvent;
import com.eagle.auth.core.domain.port.OnlineUserPort;
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
 * 账号安全事件处理器:执行级联副作用 + 技术细节日志。
 *
 * <p>三类事件:
 * <ul>
 *   <li>冻结 → 强制下线该账号所有在线 token</li>
 *   <li>解冻 → 仅打印结构化执行日志</li>
 *   <li>删除 → 强制下线该账号所有在线 token</li>
 * </ul>
 *
 * <p>用户操作的审计(谁在何时冻结/解冻/删除)由 {@code @AuditLog} 切面在
 * {@code AccountApplicationService} 主线程内完成,写入 {@code eagle_audit_log} 表;
 * 本处理器在异步线程内输出"实际下线了多少 jti"等技术细节,走 SLF4J 收敛到 ELK,
 * 两者职责互补,不重叠。
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

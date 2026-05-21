package com.eagle.example.integration.seata;

import com.eagle.seata.transaction.GlobalTransactionTemplate;
import com.eagle.seata.transaction.TransactionCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Seata Starter 验证服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eagle.seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataVerificationService {

    private final GlobalTransactionTemplate globalTransactionTemplate;

    public String executeInGlobalTx() {
        return globalTransactionTemplate.execute("example-verify", new TransactionCallback<String>() {
            @Override
            public String doInTransaction() {
                log.info("[Seata] Executing in global transaction");
                return "tx-success";
            }
        });
    }
}

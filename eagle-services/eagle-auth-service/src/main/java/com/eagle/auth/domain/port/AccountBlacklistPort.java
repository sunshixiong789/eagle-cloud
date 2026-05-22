package com.eagle.auth.domain.port;

import java.util.Optional;

/**
 * 账号黑名单查询端口。
 */
public interface AccountBlacklistPort {

    /**
     * 查询账号当前生效的黑名单记录。
     *
     * @param accountId 认证账号 ID
     * @return 生效黑名单信息
     */
    Optional<AccountBlacklistInfo> findAccountBlacklist(Long accountId);
}

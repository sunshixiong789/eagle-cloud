package com.eagle.system.auth.infrastructure.adapter;

import com.eagle.system.auth.domain.port.OnlineUserInfo;
import com.eagle.system.auth.domain.port.OnlineUserPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link OnlineUserPort} 的空实现（当 Redis 不可用时的降级方案）。
 *
 * <p>此实现在以下情况生效：
 * <ul>
 *   <li>没有 Redis 连接时（OnlineUserAdapter 创建失败）</li>
 *   <li>或通过配置 {@code eagle.online-user.enabled=false} 显式禁用</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@ConditionalOnMissingBean(OnlineUserAdapter.class)
public class NoOpOnlineUserAdapter implements OnlineUserPort {

    @Override
    public void trackLogin(OnlineUserInfo info) {
        log.debug("Online user tracking disabled (no Redis available), skipping trackLogin for tokenId: {}", info.tokenId());
    }

    @Override
    public List<OnlineUserInfo> listOnlineUsers() {
        log.debug("Online user tracking disabled (no Redis available), returning empty list");
        return List.of();
    }

    @Override
    public void forceLogout(String tokenId) {
        log.debug("Online user tracking disabled (no Redis available), skipping forceLogout for tokenId: {}", tokenId);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        log.debug("Online user tracking disabled (no Redis available), defaulting isBlacklisted to false for jti: {}", jti);
        return false;
    }
}

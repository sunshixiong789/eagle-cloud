package com.eagle.auth.infrastructure.adapter;

import com.eagle.auth.domain.model.enums.BlacklistType;
import com.eagle.auth.domain.port.AccountBlacklistInfo;
import com.eagle.auth.domain.port.AccountBlacklistPort;
import com.eagle.auth.domain.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * {@link AccountBlacklistPort} 的 JPA 实现。
 */
@Component
@RequiredArgsConstructor
public class AccountBlacklistAdapter implements AccountBlacklistPort {

    private final BlacklistRepository blacklistRepository;

    @Override
    public Optional<AccountBlacklistInfo> findAccountBlacklist(Long accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        return blacklistRepository
                .findByTypeAndValue(BlacklistType.ACCOUNT_ID, accountId.toString())
                .filter(blacklist -> !blacklist.isExpired(LocalDateTime.now()))
                .map(blacklist -> new AccountBlacklistInfo(blacklist.getId(), blacklist.getValue()));
    }
}

package com.eagle.monolith.integration;

import com.eagle.auth.domain.AuthErrorCode;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.port.AccountBlacklistPort;
import com.eagle.auth.domain.port.AuthorizationInfo;
import com.eagle.auth.domain.port.AuthorizationPort;
import com.eagle.auth.domain.port.OnlineUserInfo;
import com.eagle.auth.domain.port.OnlineUserPort;
import com.eagle.auth.domain.repository.AccountRepository;
import com.eagle.common.event.BaseEvent;
import com.eagle.rocketmq.publisher.DomainEventPublisher;
import com.eagle.system.base.application.service.AuthorizationQueryService;
import com.eagle.system.base.infrastructure.remote.AuthAccountBlacklistClient;
import com.eagle.system.base.infrastructure.remote.AuthAccountClient;
import com.eagle.system.base.infrastructure.remote.AuthOnlineUserClient;
import com.eagle.system.base.infrastructure.remote.dto.AccountBlacklistSnapshot;
import com.eagle.system.base.infrastructure.remote.dto.AccountSnapshot;
import com.eagle.system.base.infrastructure.remote.dto.OnlineUserSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Replaces cross-service HTTP/MQ integration with in-process calls for the monolith runtime.
 */
@Configuration(proxyBeanMethods = false)
public class MonolithLocalIntegrationConfiguration {

    @Bean
    @Primary
    public AuthorizationPort monolithAuthorizationPort(AuthorizationQueryService authorizationQueryService) {
        return accountId -> authorizationQueryService.findByAccountId(accountId)
                .map(view -> new AuthorizationInfo(view.name(), view.avatar(), view.roleCodes()));
    }

    @Bean
    @Primary
    public AuthAccountClient monolithAuthAccountClient(AccountRepository accountRepository) {
        return username -> {
            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);
            return new AccountSnapshot(account.getId(), account.getUsername(), account.getPhone());
        };
    }

    @Bean
    @Primary
    public AuthAccountBlacklistClient monolithAuthAccountBlacklistClient(
            AccountBlacklistPort accountBlacklistPort) {
        return accountId -> accountBlacklistPort.findAccountBlacklist(accountId)
                .map(info -> ResponseEntity.ok(new AccountBlacklistSnapshot(info.id(), info.value())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Bean
    @Primary
    public AuthOnlineUserClient monolithAuthOnlineUserClient(OnlineUserPort onlineUserPort) {
        return new AuthOnlineUserClient() {
            @Override
            public List<OnlineUserSnapshot> listOnlineUsers() {
                return onlineUserPort.listOnlineUsers().stream()
                        .map(MonolithLocalIntegrationConfiguration::toSnapshot)
                        .toList();
            }

            @Override
            public List<String> listJtisByAccount(Long accountId) {
                return onlineUserPort.listJtisByAccount(accountId);
            }

            @Override
            public void forceLogout(String tokenId) {
                onlineUserPort.forceLogout(tokenId);
            }
        };
    }

    @Bean
    @Primary
    public DomainEventPublisher monolithDomainEventPublisher() {
        return new NoopDomainEventPublisher();
    }

    private static OnlineUserSnapshot toSnapshot(OnlineUserInfo info) {
        return new OnlineUserSnapshot(
                info.tokenId(),
                info.userId(),
                info.username(),
                info.ip(),
                info.loginTime(),
                info.lastActiveTime(),
                info.browser(),
                info.os(),
                info.expiresIn());
    }

    private static class NoopDomainEventPublisher implements DomainEventPublisher {

        @Override
        public <T extends BaseEvent> void publish(T event) {
        }

        @Override
        public <T extends BaseEvent> void publish(String topic, T event) {
        }

        @Override
        public <T extends BaseEvent> void publish(String topic, String tag, T event) {
        }

        @Override
        public <T extends BaseEvent> CompletableFuture<Void> publishAsync(T event) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T extends BaseEvent> CompletableFuture<Void> publishAsync(String topic, T event) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T extends BaseEvent> void publishDelayed(T event, Duration delay) {
        }

        @Override
        public <T extends BaseEvent> void publishDelayed(String topic, T event, Duration delay) {
        }

        @Override
        public <T extends BaseEvent> void publishOrdered(T event, String messageGroup) {
        }

        @Override
        public <T extends BaseEvent> void publishOrdered(String topic, T event, String messageGroup) {
        }
    }
}

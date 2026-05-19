package com.eagle.system.auth.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.auth.application.command.AddBlacklistCommand;
import com.eagle.system.auth.application.mapper.BlacklistMapper;
import com.eagle.system.auth.domain.model.Blacklist;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import com.eagle.system.auth.domain.repository.BlacklistRepository;
import com.eagle.system.auth.infrastructure.cache.BlacklistCacheStore;
import com.eagle.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistApplicationServiceTest {

    @Mock BlacklistRepository repository;
    @Mock BlacklistMapper mapper;
    @Mock BlacklistCacheStore cacheStore;
    @InjectMocks BlacklistApplicationService service;

    @BeforeEach
    void setUp() { TenantContextHolder.setTenantId("t1"); }
    @AfterEach
    void tearDown() { TenantContextHolder.clear(); }

    @Nested
    @DisplayName("addToBlacklist")
    class Add {
        @Test
        @DisplayName("should save when not duplicated")
        void shouldSave() {
            when(repository.findByTypeAndValue(BlacklistType.PHONE, "13800138000"))
                    .thenReturn(Optional.empty());
            when(repository.save(any(Blacklist.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.addToBlacklist(new AddBlacklistCommand(
                    BlacklistType.PHONE, "13800138000", "test", null, 99L, "admin"));

            verify(repository).save(any(Blacklist.class));
        }

        @Test
        @DisplayName("should throw on duplicate")
        void shouldRejectDuplicate() {
            when(repository.findByTypeAndValue(BlacklistType.PHONE, "13800138000"))
                    .thenReturn(Optional.of(Blacklist.create(
                            BlacklistType.PHONE, "13800138000", null, null, null, null)));

            assertThrows(ConflictException.class,
                    () -> service.addToBlacklist(new AddBlacklistCommand(
                            BlacklistType.PHONE, "13800138000", null, null, 99L, "admin")));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeFromBlacklist")
    class Remove {
        @Test
        @DisplayName("should throw NotFound")
        void notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.removeFromBlacklist(99L));
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlack {
        @Test
        @DisplayName("should hit cache")
        void hit() {
            when(cacheStore.isMember("t1", BlacklistType.IP, "1.1.1.1")).thenReturn(true);
            when(repository.findByTypeAndValue(BlacklistType.IP, "1.1.1.1"))
                    .thenReturn(Optional.of(Blacklist.create(
                            BlacklistType.IP, "1.1.1.1", null, null, null, null)));
            assertTrue(service.isBlacklisted(BlacklistType.IP, "1.1.1.1"));
        }
    }
}

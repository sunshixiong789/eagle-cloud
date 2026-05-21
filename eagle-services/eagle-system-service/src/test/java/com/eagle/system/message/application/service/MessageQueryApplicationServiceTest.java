package com.eagle.system.message.application.service;

import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.message.domain.model.MessageCategory;
import com.eagle.system.message.domain.model.UserMessage;
import com.eagle.system.message.domain.repository.UserMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageQueryApplicationService")
class MessageQueryApplicationServiceTest {

    private static final Long USER_ID = 100L;

    @Mock
    private UserMessageRepository userMessageRepository;

    @InjectMocks
    private MessageQueryApplicationService service;

    @Nested
    @DisplayName("markRead")
    class MarkRead {

        @Test
        @DisplayName("不存在 → NotFoundException")
        void shouldThrowWhenNotFound() {
            when(userMessageRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.markRead(USER_ID, 99L));
        }

        @Test
        @DisplayName("非本人 → DomainException")
        void shouldRejectNonOwner() {
            UserMessage m = UserMessage.create(999L, MessageCategory.SYSTEM, "t", "c", null);
            when(userMessageRepository.findById(1L)).thenReturn(Optional.of(m));
            assertThrows(DomainException.class, () -> service.markRead(USER_ID, 1L));
        }

        @Test
        @DisplayName("已读时不再 save")
        void shouldSkipWhenAlreadyRead() {
            UserMessage m = UserMessage.create(USER_ID, MessageCategory.SYSTEM, "t", "c", null);
            m.markRead();
            when(userMessageRepository.findById(1L)).thenReturn(Optional.of(m));

            service.markRead(USER_ID, 1L);

            verify(userMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("未读时 save 并置已读")
        void shouldSaveWhenUnread() {
            UserMessage m = UserMessage.create(USER_ID, MessageCategory.SYSTEM, "t", "c", null);
            when(userMessageRepository.findById(1L)).thenReturn(Optional.of(m));
            when(userMessageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.markRead(USER_ID, 1L);

            assertThat(m.isRead()).isTrue();
            verify(userMessageRepository, times(1)).save(m);
        }
    }

    @Nested
    @DisplayName("markAllRead")
    class MarkAllRead {

        @Test
        @DisplayName("应委托给 repository")
        void shouldDelegate() {
            when(userMessageRepository.markAllReadByUserId(USER_ID)).thenReturn(5);
            assertThat(service.markAllRead(USER_ID)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("countUnread")
    class CountUnread {

        @Test
        @DisplayName("应返回 repository 计数")
        void shouldReturnCount() {
            when(userMessageRepository.countByUserIdAndIsReadFalse(USER_ID)).thenReturn(3L);
            assertThat(service.countUnread(USER_ID)).isEqualTo(3L);
        }
    }
}

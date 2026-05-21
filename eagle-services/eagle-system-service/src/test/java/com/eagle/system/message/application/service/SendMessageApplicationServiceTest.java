package com.eagle.system.message.application.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendMessageApplicationService")
class SendMessageApplicationServiceTest {

    private static final Long USER_ID = 100L;

    @Mock
    private UserMessageRepository userMessageRepository;

    @InjectMocks
    private SendMessageApplicationService service;

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("无 bizKey：直接落库")
        void shouldSaveWithoutBizKey() {
            when(userMessageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UserMessage saved = service.send(USER_ID, MessageCategory.SYSTEM, "t", "c", null);

            assertThat(saved).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.isRead()).isFalse();
            verify(userMessageRepository, never()).existsByBizKey(any());
        }

        @Test
        @DisplayName("有 bizKey 且未存在：落库")
        void shouldSaveWhenBizKeyNew() {
            when(userMessageRepository.existsByBizKey("bk-1")).thenReturn(false);
            when(userMessageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UserMessage saved = service.send(USER_ID, MessageCategory.TRADE, "t", "c", "bk-1");

            assertThat(saved).isNotNull();
            assertThat(saved.getBizKey()).isEqualTo("bk-1");
        }

        @Test
        @DisplayName("有 bizKey 且已存在：跳过，返回 null")
        void shouldSkipWhenBizKeyExists() {
            when(userMessageRepository.existsByBizKey("bk-1")).thenReturn(true);

            UserMessage saved = service.send(USER_ID, MessageCategory.TRADE, "t", "c", "bk-1");

            assertThat(saved).isNull();
            verify(userMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("空白 bizKey 视同无 bizKey：直接落库")
        void shouldTreatBlankBizKeyAsAbsent() {
            when(userMessageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.send(USER_ID, MessageCategory.SYSTEM, "t", "c", "   ");

            verify(userMessageRepository, never()).existsByBizKey(any());
        }
    }
}

package com.eagle.system.message.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageCategory 解析")
class MessageCategoryTest {

    @Test
    @DisplayName("已知值返回对应枚举")
    void shouldParseKnown() {
        assertThat(MessageCategory.parse("SYSTEM")).isEqualTo(MessageCategory.SYSTEM);
        assertThat(MessageCategory.parse("trade")).isEqualTo(MessageCategory.TRADE);
        assertThat(MessageCategory.parse("  marketing  ")).isEqualTo(MessageCategory.MARKETING);
    }

    @Test
    @DisplayName("未知 / 空值降级为 SYSTEM")
    void shouldFallbackToSystem() {
        assertThat(MessageCategory.parse(null)).isEqualTo(MessageCategory.SYSTEM);
        assertThat(MessageCategory.parse("")).isEqualTo(MessageCategory.SYSTEM);
        assertThat(MessageCategory.parse("UNKNOWN_CATEGORY")).isEqualTo(MessageCategory.SYSTEM);
    }
}

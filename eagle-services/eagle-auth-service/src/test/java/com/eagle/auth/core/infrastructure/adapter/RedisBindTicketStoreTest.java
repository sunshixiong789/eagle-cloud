package com.eagle.auth.core.infrastructure.adapter;

import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.port.BindTicket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBindTicketStoreTest {

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    RedisBindTicketStore store;

    @Test
    @DisplayName("save 应以高熵 ticketId 写入 JSON 并设置 10 分钟 TTL")
    void saveShouldWriteJsonWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        String ticketId = store.save(BindTicket.ofTaobao("open-uid-1"));

        assertTrue(ticketId.length() >= 32, "ticketId 必须高熵");
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("auth:bind:ticket:" + ticketId), json.capture(),
                eq(Duration.ofMinutes(10)));
        assertTrue(json.getValue().contains("open-uid-1"));
    }

    @Test
    @DisplayName("save 两次应生成不同 ticketId")
    void saveShouldGenerateDistinctIds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        String first = store.save(BindTicket.ofTaobao("open-uid-1"));
        String second = store.save(BindTicket.ofTaobao("open-uid-1"));

        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("consume 命中应删除并还原完整字段")
    void consumeShouldGetAndDelete() {
        BindTicket ticket = BindTicket.ofWechat(
                WechatChannel.MINI_PROGRAM, "oid-1", "uid-1", "昵称", "http://a/b.png");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.getAndDelete("auth:bind:ticket:t1"))
                .thenReturn(new ObjectMapper().writeValueAsString(ticket));

        Optional<BindTicket> restored = store.consume("t1");

        assertTrue(restored.isPresent());
        assertEquals(SocialProvider.WECHAT, restored.get().provider());
        assertEquals(WechatChannel.MINI_PROGRAM, restored.get().wechatChannel());
        assertEquals("oid-1", restored.get().identifier());
        assertEquals("uid-1", restored.get().unionid());
        assertEquals("昵称", restored.get().nickname());
    }

    @Test
    @DisplayName("consume 未命中（过期/重放）应返回 empty")
    void consumeMissShouldReturnEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.getAndDelete(anyString())).thenReturn(null);

        assertTrue(store.consume("gone").isEmpty());
    }
}

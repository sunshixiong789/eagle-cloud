---
name: eagle-websocket
description: Use when implementing WebSocket/SSE/offline messaging in eagle-cloud projects — WebSocketSessionManager (sendToUser/broadcast), SseEmitterManager, OfflineMessageStore (store/getAndClear/count)
---

# eagle-websocket-starter — STOMP WebSocket + SSE + 离线消息

## 何时使用

- 站内信、客服聊天、实时通知
- 仪表盘 / 大屏数据推送
- 用户在线状态管理 + 离线消息

## 何时不要使用

- 一次性推送（HTTP 轮询即可）
- 跨服务异步消息（用 RocketMQ）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-websocket-starter')
implementation project(':eagle-starter:eagle-redis-starter')   // 离线消息 / 集群广播
```

```yaml
eagle.websocket:
  endpoint: /ws
  topic-prefix: /topic
  user-prefix: /user
  app-prefix: /app
  heartbeat-ms: 10000
  allowed-origins:
    - "https://eagle-admin.example.com"
```

## 核心 API

| 类 / 接口                              | 用途                                                                                                                                                                 |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EagleWebSocketConfig`              | STOMP 端点 + Broker 配置（自动注册）                                                                                                                                         |
| `WebSocketAuthHandshakeInterceptor` | 握手期 JWT 鉴权                                                                                                                                                         |
| `WebSocketChannelInterceptor`       | 订阅通道拦截                                                                                                                                                             |
| `WebSocketEventListener`            | Connect / Disconnect 事件                                                                                                                                            |
| `WebSocketSessionManager`           | `sendToUser(userId, destination, payload)` / `broadcast(destination, payload)`                                                                                     |
| `OfflineMessageStore`               | `store(userId, message, ttl)` / `getAndClear(userId)` 返回 `List<String>` / `count(userId)`                                                                          |
| `RedisOfflineMessageStore`          | 默认实现                                                                                                                                                               |
| `OfflineMessage`                    | 离线消息载荷                                                                                                                                                             |
| `SseEmitterManager`                 | SSE：`connect(userId [, timeoutMs])` / `sendToUser(userId, event, payload)` / `broadcast(event, payload)` / `getConnectionCount(userId)` / `disconnectUser(userId)` |
| `WebSocketMetrics`                  | Micrometer 指标埋点                                                                                                                                                    |

## 最小示例

### WebSocket / STOMP

```java

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final WebSocketSessionManager sessionManager;
    private final OfflineMessageStore offlineStore;
    private final ObjectMapper objectMapper;

    public void notifyUser(Long userId, NotifyDto dto) throws Exception {
        // 1) 推送（在线则收到）
        sessionManager.sendToUser(userId.toString(), "/queue/notify", dto);

        // 2) 同时存离线（保证用户上线后能看到）
        // 实际策略：可由业务方决定"在线则不存离线"
        offlineStore.store(userId.toString(),
                objectMapper.writeValueAsString(dto),
                Duration.ofDays(7));
    }

    /** 用户重连时取出离线消息 */
    public List<NotifyDto> drainOffline(Long userId) throws Exception {
        List<String> raw = offlineStore.getAndClear(userId.toString());
        return raw.stream()
                .map(s -> {
                    try {
                        return objectMapper.readValue(s, NotifyDto.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /** 广播 */
    public void broadcastAnnouncement(Announcement msg) {
        sessionManager.broadcast("/topic/announcement", msg);
    }
}

// 客户端（前端 stomp.js）
// const stomp = Stomp.over(new SockJS('/ws'));
// stomp.connect({Authorization: 'Bearer ' + token}, () => {
//     stomp.subscribe('/user/queue/notify', m => console.log(m.body));
//     stomp.subscribe('/topic/announcement', m => console.log(m.body));
// });
```

### Server-Sent Events（SSE，单向流）

```java

@RestController
@RequiredArgsConstructor
public class StreamController {

    private final SseEmitterManager sseManager;

    /** 客户端订阅：浏览器 EventSource 或 fetch + ReadableStream */
    @GetMapping(value = "/api/stream/{userId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String userId) {
        return sseManager.connect(userId, 60_000);   // 60s 超时
    }
}

// 业务侧推送
sseManager.

sendToUser(userId, "order-update",orderDto);
sseManager.

broadcast("system-status",status);

// 强制下线
sseManager.

disconnectUser(userId);
```

## 配置项

| key                               | 类型     | 默认       | 说明              |
|-----------------------------------|--------|----------|-----------------|
| `eagle.websocket.endpoint`        | String | `/ws`    | STOMP 端点        |
| `eagle.websocket.topic-prefix`    | String | `/topic` | 广播前缀            |
| `eagle.websocket.user-prefix`     | String | `/user`  | 用户专属前缀          |
| `eagle.websocket.app-prefix`      | String | `/app`   | 客户端发送应用前缀       |
| `eagle.websocket.heartbeat-ms`    | long   | `10000`  | 心跳间隔            |
| `eagle.websocket.allowed-origins` | List   | `[*]`    | 跨域来源（生产必须改具体域名） |

⚠️ **没有 `enabled` / `message-size-limit` / `offline-message.enabled / ttl` 等**——离线消息 TTL 由业务方调用 `store()`
时传入。

## 集群部署

多实例需用 Redis Pub/Sub（`eagle-redis-starter` 的 `RedissonTopicUtil`）做跨实例消息分发；离线消息走
`RedisOfflineMessageStore`（默认）。

## 常见错误

- ❌ 推送大对象 → ✅ 推送 ID + 客户端拉详情
- ❌ 握手不鉴权 → ✅ `WebSocketAuthHandshakeInterceptor` 校验 JWT
- ❌ 订阅 Topic 不鉴权 → ✅ `WebSocketChannelInterceptor` 校验
- ❌ 不限制单连接订阅数 → ✅ 业务层限制
- ❌ `allowed-origins` 生产用 `*` → ✅ 配具体域名
- ❌ 期望默认 TTL → ✅ 离线消息 TTL 由 `store()` 调用时显式传入
- ❌ 配置写 `offline-message.enabled` → ✅ 没有此字段（OfflineMessageStore 直接调用即可）

## 关联规则

- `.claude/rules/05-security.md`

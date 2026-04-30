# eagle-websocket-starter — WebSocket / SSE（实时推送 + 离线消息）

## 何时使用

- 站内信、客服聊天、实时通知
- 仪表盘 / 大屏数据实时推送
- 在线协作（多人编辑、白板）
- 用户在线状态管理

## 何时不要使用

- 一次性推送（用 HTTP 轮询 / 短连接即可）
- 跨服务异步消息（用 RocketMQ）
- 单向高吞吐推送（考虑 SSE 比 WS 更轻量）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-websocket-starter')
implementation project(':eagle-starter:eagle-redis-starter')   // 离线消息存储依赖 Redis
```

```yaml
eagle.websocket:
  enabled: true
  endpoint: /ws                        # WebSocket 端点
  allowed-origins:
    - https://eagle-admin.example.com
  heartbeat-interval-seconds: 30
  message-size-limit: 64KB
  offline-message:
    enabled: true
    ttl: 7d                            # 离线消息保留时长
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `EagleWebSocketConfig` | STOMP 端点 + Broker 配置 |
| `WebSocketSessionManager` | 会话管理：`getOnlineUsers` / `sendToUser` / `broadcast` |
| `WebSocketAuthHandshakeInterceptor` | 握手期 JWT 鉴权 |
| `WebSocketChannelInterceptor` | 消息通道拦截（订阅鉴权） |
| `WebSocketEventListener` | 连接 / 断开事件 |
| `OfflineMessageStore` | 离线消息存储抽象 |
| `RedisOfflineMessageStore` | Redis 实现（TTL + 用户分组） |
| `OfflineMessage` | 离线消息载荷 |
| `SseEmitterManager` | SSE 流推送管理（`createEmitter` / `send`） |
| `WebSocketMetrics` | 指标埋点（连接数、消息量、平均延迟） |

## 最小示例

### WebSocket / STOMP

```java
// 1) 服务端推送
@RequiredArgsConstructor
@Service
public class NotificationService {
    private final WebSocketSessionManager sessionManager;
    private final OfflineMessageStore offlineStore;

    public void notifyUser(Long userId, String message) {
        boolean online = sessionManager.sendToUser(userId, "/topic/notify", message);
        if (!online) {
            offlineStore.save(userId, OfflineMessage.of("/topic/notify", message));
        }
    }

    public void broadcastAll(String message) {
        sessionManager.broadcast("/topic/global", message);
    }
}

// 2) 客户端连接（前端 stomp.js）
const stomp = Stomp.over(new SockJS('/ws'));
stomp.connect({Authorization: 'Bearer ' + token}, () => {
    stomp.subscribe('/topic/notify', msg => console.log(msg.body));
});

// 3) 用户上线时推送离线消息（自动）
// WebSocketEventListener 监听 SessionConnected 事件，自动 flush 离线消息
```

### Server-Sent Events（SSE，单向流）

```java
@RestController
public class StreamController {

    private final SseEmitterManager sseManager;

    @GetMapping(value = "/api/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long userId) {
        return sseManager.createEmitter(userId, Duration.ofMinutes(30));
    }

    /** 业务侧推送 */
    public void push(Long userId, Object event) {
        sseManager.send(userId, "update", event);
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.websocket.enabled` | boolean | `true` | 总开关 |
| `eagle.websocket.endpoint` | String | `/ws` | WS 端点路径 |
| `eagle.websocket.allowed-origins` | List | — | 允许的来源 |
| `eagle.websocket.heartbeat-interval-seconds` | int | `30` | 心跳间隔 |
| `eagle.websocket.message-size-limit` | DataSize | `64KB` | 单消息上限 |
| `eagle.websocket.offline-message.enabled` | boolean | `true` | 离线消息存储 |
| `eagle.websocket.offline-message.ttl` | Duration | `7d` | 离线消息 TTL |

## 集群部署

多实例场景**必须**：
- 用 Redis Pub/Sub（`eagle-redis-starter` 的 `RedissonTopicUtil`）做跨实例消息分发
- 离线消息走 `RedisOfflineMessageStore`（默认）
- Sticky Session（K8s Service Affinity）或共享 session

## 常见错误

- ❌ 推送时不判断是否在线 → ✅ 离线时落库 `OfflineMessageStore`
- ❌ 握手期不鉴权 → ✅ `WebSocketAuthHandshakeInterceptor` 校验 JWT
- ❌ 订阅 Topic 不鉴权 → ✅ `WebSocketChannelInterceptor` 校验订阅权限
- ❌ 推送大对象 → ✅ 推送 ID + 客户端拉取详情
- ❌ 不限制单连接订阅数 → ✅ 服务端限制（防资源耗尽）
- ❌ 没有心跳 → ✅ 配置 `heartbeat-interval-seconds`

## 关联规则

- `.claude/rules/12-security.md` — 握手鉴权
- `.claude/rules/14-cache.md` — 离线消息 Redis Key 命名
- `.claude/rules/24-deployment.md` — 集群部署 Sticky Session

# System 模块 — WebSocket 消息接口文档

> Swagger（OpenAPI 3.x）不支持 WebSocket/STOMP 等长连接接口，因此本模块的实时消息接口通过本文档单独维护。
> 规范层使用 [AsyncAPI 3.0](https://www.asyncapi.com/)（事件驱动 API 的事实标准），并附人类可读的快速参考。

---

## 1. 连接信息

| 项                            | 值                                                               |
|------------------------------|-----------------------------------------------------------------|
| Endpoint（STOMP over SockJS）  | `/ws-stomp`                                                     |
| 完整 URL（直连）                   | `ws://{host}:{port}/ws-stomp`                                   |
| 完整 URL（经网关）                  | `wss://{gateway-host}/system/ws-stomp`                          |
| 协议                           | STOMP 1.2 over WebSocket，支持 SockJS 降级                           |
| 客户端消息发送前缀（`@MessageMapping`） | `/message`                                                      |
| 广播订阅前缀                       | `/topic`                                                        |
| 点对点订阅前缀（用户专属）                | `/user`（实际下行：`/user/queue/xxx`）                                 |
| 心跳                           | 10 000 ms（服务端 ↔ 客户端）                                            |
| 跨域                           | `application.yml: eagle.websocket.allowed-origins`（生产必须收敛到具体域名） |

### 鉴权（握手阶段）

仅支持 **JWT Bearer Token** 一种鉴权方式。握手必须携带有效的 JWT，token 的传输位置有两个候选（二选一，按下列优先级匹配）：

1. URL 查询参数：`/ws-stomp?token={JWT}`（浏览器场景，因 WebSocket 握手不允许自定义 Header）
2. HTTP Header：`Authorization: Bearer {JWT}`（非浏览器客户端）

服务端由 `WebSocketAuthHandshakeInterceptor` 解析 token 并放入 session attributes，校验失败的连接会被拒绝。

---

## 2. 客户端 → 服务端（PUBLISH）

| 操作     | 目的地（含 appPrefix）             | Payload                             | 触发的下行                          |
|--------|------------------------------|-------------------------------------|--------------------------------|
| 发送广播消息 | `/message/broadcast-message` | [`ChatMessage`](#chatmessage)       | 推送到 `/topic/public`            |
| 发送私信   | `/message/message-to-one`    | [`PrivateMessage`](#privatemessage) | 推送到目标用户的 `/user/queue/private` |

### 校验规则

| 校验失败           | 错误码     | i18n key                        | 默认消息     |
|----------------|---------|---------------------------------|----------|
| 广播 / 私信内容为空或空白 | `13005` | `error.chat.message_required`   | 消息内容不能为空 |
| 私信缺少收件人 `to`   | `13006` | `error.chat.recipient_required` | 接收者不能为空  |

> 错误通过 `@MessageExceptionHandler` 记录到服务端日志；当前实现**未**把错误回推给客户端，前端需通过业务确认/超时来感知失败。

---

## 3. 服务端 → 客户端（SUBSCRIBE）

| 订阅目的地                 | 谁会收到    | Payload                             | 触发来源                                                                                   |
|-----------------------|---------|-------------------------------------|----------------------------------------------------------------------------------------|
| `/topic/public`       | 所有订阅者   | [`ChatMessage`](#chatmessage)       | 客户端调用 `/message/broadcast-message`，或服务端业务代码调用 `WebSocketSessionManager.broadcast(...)` |
| `/user/queue/private` | 仅当前登录用户 | [`PrivateMessage`](#privatemessage) | 其他用户发送 `/message/message-to-one`                                                       |

> 自定义业务推送可继续按 `eagle-websocket-starter` 的 `WebSocketSessionManager.broadcast / sendToUser` 扩展；
> 新增 destination 时请同步更新本文档。

---

## 4. 数据模型

### ChatMessage

```json
{
  "content": "大家好",
  "sender": "alice"
}
```

| 字段        | 类型     | 必填 | 说明                      |
|-----------|--------|----|-------------------------|
| `content` | string | ✅  | 消息正文；空白将被拒绝（`13005`）    |
| `sender`  | string | ❌  | 发送者标识；如未填，服务端会忽略（不强制覆写） |

### PrivateMessage

```json
{
  "to": "1024",
  "content": "你好",
  "from": "alice"
}
```

| 字段        | 类型     | 必填 | 说明                                |
|-----------|--------|----|-----------------------------------|
| `to`      | string | ✅  | 收件人用户 ID;空白将被拒绝（`13006`）          |
| `content` | string | ✅  | 消息正文；空白将被拒绝（`13005`）              |
| `from`    | string | ❌  | 发送者标识；建议服务端基于 `Principal` 覆写（防伪造） |

---

## 5. AsyncAPI 3.0 规范

存放于同目录下的 `websocket-api.yaml`，可用以下工具渲染：

- AsyncAPI Studio: <https://studio.asyncapi.com/>
- `npx @asyncapi/cli start studio --file websocket-api.yaml`
- AsyncAPI Generator → HTML 文档：`ag websocket-api.yaml @asyncapi/html-template -o build/asyncapi`

---

## 6. 前端接入示例（SockJS + StompJS）

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () =>
    new SockJS(`/system/ws-stomp?token=${encodeURIComponent(accessToken)}`),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  debug: (msg) => console.debug('[STOMP]', msg),
});

client.onConnect = () => {
  // 订阅广播
  client.subscribe('/topic/public', (frame) => {
    const msg = JSON.parse(frame.body);
    console.log('broadcast:', msg);
  });

  // 订阅私信（注意 /user 前缀由服务端自动注入，订阅时直接写 /user/queue/...）
  client.subscribe('/user/queue/private', (frame) => {
    const msg = JSON.parse(frame.body);
    console.log('private:', msg);
  });

  // 发送广播
  client.publish({
    destination: '/message/broadcast-message',
    body: JSON.stringify({ content: '大家好', sender: 'alice' }),
  });

  // 发送私信
  client.publish({
    destination: '/message/message-to-one',
    body: JSON.stringify({ to: '1024', content: '你好', from: 'alice' }),
  });
};

client.activate();
```

---

## 7. 自测命令

```bash
# wscat（裸 WebSocket，不走 SockJS）
wscat -c "ws://localhost/ws-stomp/websocket?token=${ACCESS_TOKEN}"

# 一旦连上，按 STOMP 1.2 帧格式手工发送 CONNECT / SEND / SUBSCRIBE 帧（仅做联通性验证）
```

---

## 8. 变更记录

| 日期         | 变更                                                     | 作者        |
|------------|--------------------------------------------------------|-----------|
| 2026-05-15 | 初版：覆盖 ChatController 的 broadcast / point-to-point 两个端点 | system 模块 |

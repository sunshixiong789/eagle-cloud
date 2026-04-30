# eagle-notification-starter — 多渠道消息通知（短信 / 邮件 / 模板）

## 何时使用

- 验证码、订单通知、营销消息（短信 / 邮件）
- 多渠道统一发送（一次调用，自动按用户偏好分发）
- 模板化消息（占位符替换）

## 何时不要使用

- 站内信 / 系统通知（用 `eagle-websocket-starter` 或自建表）
- 跨服务异步消息（用 `eagle-rocketmq-starter`）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-notification-starter')
```

```yaml
eagle.message:
  enabled: true
  default-channels:
    - SMS
    - EMAIL
  sms:
    provider: aliyun                   # aliyun / tencent / 自定义
    aliyun:
      access-key-id: ${SMS_AK}
      access-key-secret: ${SMS_SK}
      sign-name: 鹰云
  email:
    from: noreply@eagle.com
    smtp-host: smtp.example.com
    smtp-port: 587
    username: ${MAIL_USER}
    password: ${MAIL_PASSWORD}
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `NotificationService` | 统一发送门面（`send(MessageDTO)`） |
| `MessageChannel` | 渠道接口（业务可扩展自定义） |
| `EmailMessageChannel` | 邮件实现（Spring Mail） |
| `SmsMessageChannel` | 短信实现（阿里云） |
| `MessageChannelType` | 枚举：`SMS` / `EMAIL` / `WECHAT` / `INTERNAL` |
| `MessageDTO` | 消息载荷（接收人、模板 ID、变量、渠道） |
| `MessageTemplateEngine` | 模板渲染（占位符替换） |
| `MessageErrorCode` | 错误码 |

## 最小示例

```java
@RequiredArgsConstructor
@Service
public class OrderNotificationService {

    private final NotificationService notification;

    public void notifyOrderShipped(Long userId, String orderNo, String trackingNo) {
        MessageDTO msg = MessageDTO.builder()
            .receiverId(userId)
            .channels(List.of(MessageChannelType.SMS, MessageChannelType.EMAIL))
            .templateId("order.shipped")
            .variables(Map.of(
                "orderNo", orderNo,
                "trackingNo", trackingNo
            ))
            .build();

        notification.send(msg);
    }

    /** 验证码（仅短信）*/
    public void sendVerifyCode(String mobile, String code) {
        MessageDTO msg = MessageDTO.builder()
            .receiverMobile(mobile)
            .channels(List.of(MessageChannelType.SMS))
            .templateId("verify.code")
            .variables(Map.of("code", code, "minutes", "5"))
            .build();
        notification.send(msg);
    }
}
```

模板（如 `verify.code`）：

```
您的验证码是 {{code}}，有效期 {{minutes}} 分钟。请勿泄露。
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.message.enabled` | boolean | `true` | 总开关 |
| `eagle.message.default-channels` | List | `[SMS, EMAIL]` | 默认渠道 |
| `eagle.message.sms.provider` | enum | `aliyun` | 短信服务商 |
| `eagle.message.sms.aliyun.access-key-id` | String | — | 阿里云 AK（ENC()） |
| `eagle.message.email.from` | String | — | 发件邮箱 |
| `eagle.message.async-enabled` | boolean | `true` | 异步发送 |

## 自定义渠道

实现 `MessageChannel` 即可：

```java
@Component
public class WechatMpChannel implements MessageChannel {
    @Override public MessageChannelType type() { return MessageChannelType.WECHAT; }
    @Override public void send(MessageDTO message) { /* 调用微信 API */ }
}
```

## 常见错误

- ❌ 同步阻塞发送 → ✅ 默认异步（`async-enabled: true`）
- ❌ 验证码用 `Random` → ✅ `SecureRandom`，详见 `12-security.md`
- ❌ 把 AK/SK 明文写到配置 → ✅ ENC() 加密
- ❌ 验证码无频率限制 → ✅ 用 `eagle-redis-starter` 的 `RedisRateLimiter`（5/min/手机号）
- ❌ 营销消息无退订 → ✅ 模板含退订指引（合规要求）

## 关联规则

- `.claude/rules/12-security.md` — 验证码安全 / 频率限制
- `.claude/rules/19-config.md` — 凭证加密
- `.claude/rules/20-i18n.md` — 多语言模板

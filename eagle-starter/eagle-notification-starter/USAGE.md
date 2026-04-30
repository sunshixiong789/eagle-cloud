# eagle-notification-starter — 短信 / 邮件 / 站内信通知（模板渲染 + 多渠道）

## 何时使用

- 短信验证码 / 通知（阿里云）
- 邮件通知（Spring Mail）
- 站内信（IN_APP）
- 模板化消息（`${key}` 占位符）

## 何时不要使用

- 跨服务异步消息 → 用 RocketMQ
- 实时推送 → 用 `eagle-websocket-starter`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-notification-starter')
```

```yaml
spring.mail:                              # type=EMAIL 时
  host: smtp.example.com
  port: 587
  username: ${MAIL_USER}
  password: ${MAIL_PASSWORD}

eagle.message:
  enabled: true
  sms:
    access-key-id: ${SMS_AK}
    access-key-secret: ${SMS_SK}
    sign-name: 鹰云
    endpoint: dysmsapi.aliyuncs.com
  email:
    from: noreply@eagle.com
  templates:                              # 模板配置（key 是模板编码）
    verify.code:
      subject: 您的验证码
      content: "您的验证码是 ${code}，有效期 ${minutes} 分钟。"
      sms-template-id: SMS_123456789       # 阿里云模板 ID（SMS 渠道）
    order.shipped:
      subject: 订单已发货
      content: "订单 ${orderNo} 已发货，物流 ${trackingNo}"
```

## 核心 API

| 类 / 接口 | 说明 |
|---|---|
| `NotificationService` | `send(MessageDTO)` 同步 / `sendAsync(MessageDTO)` 异步（用 `messageTaskExecutor` 池）|
| `MessageDTO` | record：`recipients(Set<String>)` + `templateCode` + `params(Map<String,String>)` + `channelType(MessageChannelType)` |
| `MessageChannelType` | 枚举：`SMS / EMAIL / IN_APP` |
| `MessageChannel` | 渠道接口：`supports(type)` + `send(message, content)` 业务可扩展 |
| `EmailMessageChannel` / `SmsMessageChannel` | 默认实现 |
| `MessageTemplateEngine` | 模板渲染（`${key}` → `params.get(key)`） |
| `MessageErrorCode` | 错误码：`EMPTY_RECIPIENTS / TEMPLATE_NOT_FOUND / CHANNEL_NOT_SUPPORTED` |

## 最小示例

```java
@RequiredArgsConstructor
@Service
public class OrderNotificationService {

    private final NotificationService notification;

    /** 短信验证码 */
    public void sendVerifyCode(String mobile, String code) {
        MessageDTO msg = new MessageDTO(
            Set.of(mobile),
            "verify.code",
            Map.of("code", code, "minutes", "5"),
            MessageChannelType.SMS
        );
        notification.send(msg);
    }

    /** 邮件 — 异步 */
    public void notifyOrderShipped(String email, String orderNo, String trackingNo) {
        MessageDTO msg = new MessageDTO(
            Set.of(email),
            "order.shipped",
            Map.of("orderNo", orderNo, "trackingNo", trackingNo),
            MessageChannelType.EMAIL
        );
        notification.sendAsync(msg);
    }
}
```

## 自定义渠道

```java
@Component
public class WechatMpChannel implements MessageChannel {
    @Override public boolean supports(MessageChannelType type) {
        return type == MessageChannelType.IN_APP;     // 或扩展枚举
    }

    @Override public void send(MessageDTO message, String content) {
        // 调用微信公众号 API
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.message.enabled` | boolean | `true` | 总开关 |
| `eagle.message.sms.access-key-id` | String | — | 阿里云 AK（ENC()） |
| `eagle.message.sms.access-key-secret` | String | — | 阿里云 SK（ENC()） |
| `eagle.message.sms.sign-name` | String | — | 短信签名 |
| `eagle.message.sms.endpoint` | String | `dysmsapi.aliyuncs.com` | 阿里云 SMS 端点 |
| `eagle.message.email.from` | String | — | 发件邮箱 |
| `eagle.message.templates.{code}.subject` | String | — | 模板主题（邮件） |
| `eagle.message.templates.{code}.content` | String | — | 模板内容 |
| `eagle.message.templates.{code}.sms-template-id` | String | — | 阿里云 SMS 模板 ID |

邮件 SMTP 配置走 Spring Boot 标准 `spring.mail.*`。

## 常见错误

- ❌ `MessageDTO.builder().receiverId(...)` → ✅ MessageDTO 是 record：`recipients(Set<String>) / templateCode / params / channelType`
- ❌ `MessageChannelType.WECHAT / INTERNAL` → ✅ 真实只有 **`SMS / EMAIL / IN_APP`**
- ❌ 模板 ID 写在代码 → ✅ 配在 `eagle.message.templates.{code}.sms-template-id`
- ❌ AK/SK 明文 → ✅ ENC() 加密
- ❌ 验证码无频率限制 → ✅ 用 `eagle-redis-starter` 的限流
- ❌ 营销消息无退订指引 → ✅ 模板含退订（合规）

## 关联规则

- `.claude/rules/12-security.md` — 验证码安全
- `.claude/rules/19-config.md` — 凭证加密
- `.claude/rules/20-i18n.md` — 多语言模板

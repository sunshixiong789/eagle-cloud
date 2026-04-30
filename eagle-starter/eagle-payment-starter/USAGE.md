# eagle-payment-starter — 支付网关抽象（支付宝 / 微信支付 / 退款 / 异步通知）

## 何时使用

- 业务需要接入支付宝 / 微信支付（C 端 / B 端 / 商户号）
- 统一支付 / 退款 / 转账接口（多渠道）
- 异步通知签名校验

## 何时不要使用

- 仅记账，不接外部支付渠道
- 自建虚拟币 / 积分系统（用业务表 + 事件即可）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-payment-starter')
```

```yaml
eagle.payment:
  enabled: true
  alipay:
    enabled: true
    app-id: ${ALIPAY_APP_ID}
    private-key: ${ALIPAY_PRIVATE_KEY}    # ENC()
    alipay-public-key: ${ALIPAY_PUB_KEY}
    notify-url: https://api.eagle.com/payment/notify/alipay
  wechat:
    enabled: true
    app-id: ${WX_APP_ID}
    mch-id: ${WX_MCH_ID}
    api-v3-key: ${WX_API_V3_KEY}          # ENC()
    cert-path: classpath:wx/apiclient_cert.pem
    notify-url: https://api.eagle.com/payment/notify/wechat
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `PaymentGateway` | 网关抽象（`pay` / `refund` / `transfer` / `query`） |
| `AlipayPaymentGateway` | 支付宝实现 |
| `WechatPaymentGateway` | 微信支付实现 |
| `PaymentChannelEnum` | 渠道枚举 |
| `PayRequest` / `PayResult` | 支付请求 / 响应 |
| `RefundRequest` / `RefundResult` | 退款 |
| `TransferRequest` / `TransferResult` | 转账 |
| `PaymentNotifyController` | 异步通知接收（`/payment/notify/{channel}`） |
| `PaymentNotifyEvent` | 支付成功事件（业务订阅） |
| `PaymentSignatureValidator` | 签名校验 |
| `NotifyResult` | 通知响应（成功 / 失败 / 重试） |

## 最小示例

```java
// 1) 发起支付
@RequiredArgsConstructor
@Service
public class OrderPaymentService {
    private final Map<PaymentChannelEnum, PaymentGateway> gateways;

    public PayResult pay(Long orderId, PaymentChannelEnum channel) {
        Order order = orderRepository.findById(orderId).orElseThrow(...);

        PayRequest req = PayRequest.builder()
            .outTradeNo(order.getOrderNo())
            .totalAmount(order.getTotalAmount())
            .subject(order.getSubject())
            .channel(channel)
            .build();

        return gateways.get(channel).pay(req);
    }

    public RefundResult refund(Long orderId, BigDecimal amount, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow(...);
        return gateways.get(order.getPayChannel()).refund(
            RefundRequest.builder()
                .outTradeNo(order.getOrderNo())
                .refundAmount(amount)
                .reason(reason)
                .build()
        );
    }
}

// 2) 业务订阅支付成功事件
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    @Async("eagleTaskExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void onPaymentSucceeded(PaymentNotifyEvent event) {
        Order order = orderRepository.findByOrderNo(event.outTradeNo()).orElseThrow(...);
        order.markPaid(event.tradeNo(), event.paidAt());
    }
}
```

## 异步通知流程

```
支付宝/微信 ──HTTP POST──▶ /payment/notify/{channel}
                              │
                              ├─ PaymentSignatureValidator 验签
                              ├─ 解析为 PaymentNotifyEvent
                              ├─ 发布 Spring 事件（业务订阅）
                              └─ 返回 NotifyResult.success() / retry()
```

业务方**必须**保证事件处理幂等（按 `tradeNo` 去重）。

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.payment.enabled` | boolean | `true` | 总开关 |
| `eagle.payment.alipay.enabled` | boolean | `false` | 启用支付宝 |
| `eagle.payment.wechat.enabled` | boolean | `false` | 启用微信 |
| `eagle.payment.notify-timeout-seconds` | int | `30` | 通知处理超时 |

## 常见错误

- ❌ 异步通知不验签 → ✅ `PaymentSignatureValidator` 强校验
- ❌ 通知处理非幂等 → ✅ 按 `tradeNo` 去重
- ❌ 通知响应失败不重试 → ✅ 返回 `NotifyResult.retry()`
- ❌ 私钥 / API V3 Key 明文配置 → ✅ ENC() 加密
- ❌ 业务事务内同步发起支付 → ✅ 拆分：先建订单本地事务，再发起支付
- ❌ 退款金额 > 原支付金额 → ✅ 业务层校验 + 网关返回处理

## 关联规则

- `.claude/rules/12-security.md` — 凭证加密、签名验证
- `.claude/rules/16-transaction-distributed.md` — 支付场景的最终一致性
- `.claude/rules/15-messaging.md` — 支付完成事件下游消费
- `.claude/rules/19-config.md` — Jasypt 加密敏感字段

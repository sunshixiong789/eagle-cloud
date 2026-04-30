---
name: eagle-payment
description: Use when integrating payments (Alipay/WeChat Pay) in eagle-cloud projects — PaymentGateway interface, alipayPaymentGateway/wechatPaymentGateway beans, async notification handling via PaymentNotifyEvent, signature validation
---

# eagle-payment-starter — 统一支付网关（支付宝 / 微信支付）

## 何时使用

- 接入支付宝 / 微信支付（C 端 / 商户）
- 统一支付 / 退款 / 转账接口
- 异步通知验签 + Spring 事件分发

## 何时不要使用

- 不接外部渠道，仅记账（用业务表 + 事件）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-payment-starter')
```

```yaml
eagle.payment:
  alipay:
    app-id: ${ALIPAY_APP_ID}
    private-key: ${ALIPAY_PRIVATE_KEY}        # ENC()
    alipay-public-key: ${ALIPAY_PUB_KEY}
    server-url: https://openapi.alipay.com/gateway.do
    sign-type: RSA2
    charset: UTF-8
    notify-url: https://api.eagle.com/payment/notify/alipay
    return-url: https://app.eagle.com/payment/return
  wechat:
    mch-id: ${WX_MCH_ID}
    mch-serial-no: ${WX_MCH_SERIAL}
    private-key: ${WX_PRIVATE_KEY}            # ENC()
    api-v3-key: ${WX_API_V3_KEY}              # ENC()
    app-id: ${WX_APP_ID}
    notify-url: https://api.eagle.com/payment/notify/wechat
```

## 核心 API

```java
public interface PaymentGateway {
    PayResult pay(PayRequest request);
    RefundResult refund(RefundRequest request);
    NotifyResult parseNotify(Map<String,String> params, String body);
    PayResult queryOrder(String outTradeNo);
    default TransferResult transfer(TransferRequest request) { throw new UnsupportedOperationException(); }
    default RefundResult queryRefund(String refundNo) { throw new UnsupportedOperationException(); }
}
```

多个实现 Bean 共存：

| Bean 名 | 实现 |
|---------|------|
| `alipayPaymentGateway` | `AlipayPaymentGateway` |
| `wechatPaymentGateway` | `WechatPaymentGateway` |

注入用 `@Qualifier` 或注入 `Map<String, PaymentGateway>` 按 key 选择。

## 异步通知流程

```
支付宝/微信 ──HTTP POST──▶ PaymentNotifyController（已注册）
                              ↓
                          parseNotify（验签）
                              ↓
                          发布 PaymentNotifyEvent（Spring 事件）
                              ↓
                       业务方监听处理 + 返回 NotifyResult.success/retry
```

业务方监听事件并保证**幂等**（按 `tradeNo` 去重）。

## 最小示例

```java
// 1) 支付
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    @Qualifier("alipayPaymentGateway")
    private final PaymentGateway alipay;

    @Qualifier("wechatPaymentGateway")
    private final PaymentGateway wechat;

    public PayResult pay(Order order, PaymentChannelEnum channel) {
        PayRequest req = PayRequest.builder()
            .outTradeNo(order.getOrderNo())
            .totalAmount(order.getTotalAmount())
            .subject(order.getSubject())
            .build();

        return switch (channel) {
            case ALIPAY -> alipay.pay(req);
            case WECHAT -> wechat.pay(req);
        };
    }

    public RefundResult refund(Order order, BigDecimal amount, String reason) {
        RefundRequest req = RefundRequest.builder()
            .outTradeNo(order.getOrderNo())
            .refundAmount(amount)
            .reason(reason)
            .build();
        return alipay.refund(req);
    }
}

// 2) 监听支付通知（业务方必须幂等）
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final OrderRepository orderRepository;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void onPaymentSucceeded(PaymentNotifyEvent event) {
        // 按 tradeNo 幂等校验
        Order order = orderRepository.findByOrderNo(event.getOutTradeNo()).orElseThrow();
        if (order.isPaid()) return;
        order.markPaid(event.getTradeNo(), event.getPaidAt());
    }
}

// 3) 主动查询（对账 / 超时补偿）
PayResult result = alipay.queryOrder(orderNo);
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.payment.alipay.app-id` | String | — | 应用 ID |
| `eagle.payment.alipay.private-key` | String | — | 商户私钥 PKCS8（ENC()） |
| `eagle.payment.alipay.alipay-public-key` | String | — | 验签公钥 |
| `eagle.payment.alipay.server-url` | String | `https://openapi.alipay.com/gateway.do` | 网关地址 |
| `eagle.payment.alipay.sign-type` | String | `RSA2` | 签名类型 |
| `eagle.payment.alipay.charset` | String | `UTF-8` | 字符集 |
| `eagle.payment.alipay.notify-url` | String | — | 异步通知 URL |
| `eagle.payment.alipay.return-url` | String | — | 同步跳转 URL |
| `eagle.payment.wechat.mch-id` | String | — | 商户号 |
| `eagle.payment.wechat.mch-serial-no` | String | — | API 证书序列号 |
| `eagle.payment.wechat.private-key` | String | — | 商户私钥（ENC()） |
| `eagle.payment.wechat.api-v3-key` | String | — | APIv3 密钥（ENC()） |
| `eagle.payment.wechat.app-id` | String | — | AppId |
| `eagle.payment.wechat.notify-url` | String | — | 异步通知 URL |

## 常见错误

- ❌ 异步通知不验签 → ✅ `parseNotify` 已含验签，但务必检查 `NotifyResult` 状态
- ❌ 通知处理非幂等 → ✅ 按 `tradeNo` 去重
- ❌ 通知失败不返回 retry → ✅ 失败用 `NotifyResult.retry()`
- ❌ 私钥 / API V3 Key 明文 → ✅ ENC() 加密
- ❌ 业务事务内同步发起支付 → ✅ 拆分：先建订单，再发起支付
- ❌ 退款金额 > 原支付 → ✅ 业务层校验

## 关联规则

- `.claude/rules/12-security.md` — 凭证加密、签名验证
- `.claude/rules/16-transaction-distributed.md` — 最终一致性
- `.claude/rules/15-messaging.md` — 支付完成事件下游消费
- `.claude/rules/19-config.md` — Jasypt 加密

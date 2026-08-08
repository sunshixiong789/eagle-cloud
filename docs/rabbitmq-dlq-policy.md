# RabbitMQ DLQ 保留策略（policy）

`eagle-amqp-starter` 为每个 `AbstractDlqListener` 声明一个 `{queue}.dlq` 死信队列。DLQ 是**只进不出**的队列 ——
没有保留策略它只会一直涨，直到吃满 broker 磁盘把正常业务一起拖下水。

保留策略由 **broker 侧的 policy** 承担，**不写进队列声明**。本文说明为什么、以及怎么设。

## 为什么不写进队列声明

队列的 `arguments` 在创建后**不可变**。客户端每次启动都会重声明队列，broker 拿声明参数与现存队列做**全等比较**，
不一致就回 `406 PRECONDITION_FAILED` 并关闭 channel —— 落到 Spring 启动期就是 `AmqpIOException`，**整个服务起不来**：

```
PRECONDITION_FAILED - inequivalent arg 'x-message-ttl' for queue
'dev_trade_rebate_credited.user_invitation_rebate_credited.dlq' in vhost '/':
received the value '1209600000' of type 'signedint' but current is none
```

也就是说，把 TTL 写进声明会让「调一次保留时长」变成「停服务 → 删光所有 DLQ → 重启」，
而且每个环境都要各做一遍。policy 则可以随时改、立即对存量队列生效，是 RabbitMQ 为此提供的机制。

> ⚠️ 队列自带的 `arguments` **优先级高于 policy**。某个 DLQ 若残留 `x-message-ttl`，policy 对它不生效，
> 必须先删掉该队列让它按无参数形态重建。

## 设置 policy（每个 vhost 一次）

死信队列名统一以 `.dlq` 结尾（见 `ExchangeNaming`），所以一条 pattern 覆盖全部：

| 项 | 值 |
|---|---|
| name | `dlq-retention` |
| pattern | `.*\.dlq$` |
| apply-to | `queues` |
| definition | `{"message-ttl": 1209600000, "max-length": 100000}` |
| priority | `0` |

- `message-ttl` = 1209600000ms = **14 天**：死信是待人工处理的证据，太短会让证据在排查前就消失
- `max-length` = **10 万条**：超出后 broker 丢弃**最旧**的死信（`overflow` 默认 `drop-head`）
- 两者都由 broker 自己执行，不需要任何清理任务

### rabbitmqctl

```bash
rabbitmqctl set_policy dlq-retention '.*\.dlq$' '{"message-ttl":1209600000,"max-length":100000}' --apply-to queues --priority 0
```

### 管理 API（容器化环境用这个）

```bash
curl -s -u "$RMQ_USER:$RMQ_PASS" -X PUT "http://$RMQ_HOST:15672/api/policies/%2F/dlq-retention" -H 'content-type: application/json' -d '{"pattern":".*\\.dlq$","apply-to":"queues","priority":0,"definition":{"message-ttl":1209600000,"max-length":100000}}'
```

验证生效（每个 DLQ 应显示 `"policy": "dlq-retention"`）：

```bash
curl -s -u "$RMQ_USER:$RMQ_PASS" "http://$RMQ_HOST:15672/api/queues/%2F" | jq -r '.[] | select(.name | endswith(".dlq")) | "\(.name)\tpolicy=\(.policy // "无")\tttl参数=\(.arguments["x-message-ttl"] // "无")"'
```

### 多环境共用一个 vhost

dev / test / prod 靠 `eagle.amqp.exchange-prefix` 区分队列名前缀。若共用 vhost 且各环境要不同的保留时长，
把 pattern 收窄即可，例如 `^prod_.*\.dlq$`，并各建一条 policy。保留时长一致时用上面的 `.*\.dlq$` 一条就够。

## 清理残留了 `x-message-ttl` 的 DLQ

若某个环境曾跑过「把 TTL 写进声明」的版本（eagle-amqp-starter `1.6.0-SNAPSHOT` 的 `adc9d3a0` ~ 本次修复之间），
那批队列上会残留 `x-message-ttl`，需要删掉重建。**先看再删** —— DLQ 里有消息说明有未处理的死信，
删队列会连消息一起丢：

```bash
curl -s -u "$RMQ_USER:$RMQ_PASS" "http://$RMQ_HOST:15672/api/queues/%2F" | jq -r '.[] | select(.name | endswith(".dlq")) | select(.arguments["x-message-ttl"] != null) | "\(.name)\t消息数=\(.messages)"'
```

确认消息数为 0 后逐个删（服务会在下次启动时按无参数形态重建）：

```bash
curl -s -u "$RMQ_USER:$RMQ_PASS" -X DELETE "http://$RMQ_HOST:15672/api/queues/%2F/<队列名>?if-empty=true"
```

`if-empty=true` 是保险：队列非空时 broker 拒绝删除并返回 400，不会静默丢消息。
确有堆积的死信要先处理（管理台 "Get messages" 导出，或用 Shovel 搬到别处），处理完再删。

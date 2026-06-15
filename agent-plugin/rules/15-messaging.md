# 消息队列规范（RocketMQ）

适用 RocketMQ 5.x 与 `eagle-rocketmq-starter`。本规则保留 Topic、契约、幂等、DLQ 等项目决策。

## 何时使用 MQ

- 跨服务或跨模块最终一致。
- 削峰、异步副作用、耗时任务解耦。
- 需要事件审计、重试、补偿或延迟处理。

不用于同步查询、强一致读写、简单方法调用替代。

## Topic / Tag

- Topic：`{env}_{domain}_{event}`，例如 `prod_order_events`。
- Tag：业务动作或事件类型，例如 `created`、`paid`、`cancelled`。
- Topic 只能包含字母、数字、下划线、短横线；遵守 starter 校验。
- 不同服务相同 domain 可能冲突时，Topic 必须带服务名。

## 发布

- 禁止直接裸用 `RocketMQTemplate`；通过 `DomainEventPublisher`、`TransactionalEventPublisher` 或 starter 封装发布。
- 领域事件先由聚合根注册，本地事务提交后再转换为 MQ 集成事件。
- 事务消息只用于“本地事务 + MQ 发送”需要强绑定的场景。

## 集成事件契约

- 生产方负责文档化 Topic、Tag、版本、字段、示例和消费方。
- 消费方不要 `import` 生产方事件类；各自定义 `XxxMessage`，靠 JSON 字段兼容。
- 字段只允许向后兼容新增；重命名、删除、类型变更必须升版本并灰度。
- 消息体禁止包含密码、Token、密钥、完整证件号等敏感字段。
- 事件字段至少包含：`eventId`、`occurredOn`、业务主键、版本。

## 消费者

- 继承 `AbstractRocketMqListener<T>`，实现 `getTopic()`、`getEventClass()`、`handle(T event)`。
- 子类构造器显式调用 `super(rocketMqProperties)`；不要用 Lombok `@RequiredArgsConstructor` 代替。
- Consumer 只做反序列化、日志、转发；业务幂等放在 ApplicationService。
- 禁止使用 `@RocketMQMessageListener` 绕过 starter。

## 幂等

消费者必须幂等。幂等 key 使用消息自带 `eventId`，不要用 MQ `MsgId`。

优先级：

1. 创建型：业务表唯一约束。
2. 状态机推进：条件 UPDATE + 二次 SELECT 区分已处理、缺失和异常状态。
3. 累加型：业务专属事实表记录 eventId。
4. 纯副作用：Redis SETNX 前置守卫，设置合理 TTL。

禁止用通用 inbox 表替代业务幂等；它会制造双写和清理问题。

## DLQ / 重试 / 顺序

- 死信必须有 `AbstractDlqListener` 或等价处理，并接入告警。
- 重试只处理临时故障；业务不可恢复错误应记录并进入补偿流程。
- 顺序消息只在状态机等确需顺序时使用，按业务 ID 设置 message group。
- 默认优先用“事件版本号 + 幂等”替代顺序消费。

## 监控

每个 Topic 监控堆积量、消费延迟、失败率、DLQ 数量和消费耗时。

## 禁止清单

- 裸用 `RocketMQTemplate`。
- 消费方依赖生产方内部事件类。
- 非幂等消费。
- 裸 `catch (DataIntegrityViolationException) { return; }` 吞并发异常。
- 死信静默吞掉。
- 事务中同步等待 MQ 消费结果。

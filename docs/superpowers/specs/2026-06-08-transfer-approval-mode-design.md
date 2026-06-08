# Transfer 提现：审核模式 + 立即到账双受理路径

- 日期：2026-06-08
- 作者：sunshixiong（与 Claude Code 协作 brainstorm）
- 状态：设计稿，待评审 → writing-plans
- 影响范围：`eagle-payment-service` 的 Transfer 聚合 / Application / Controller / 事件 / 错误码

## 1. 背景与目标

当前 `eagle-payment-service` 的 Transfer（提现 / B2C 转账）聚合实现的是单一路径：
`PENDING → REVIEWING → SUCCESS/FAILED/RETURNED`，其中 `REVIEWING` 的实际语义是
"已提交渠道、等待渠道异步回调"，并非人工审核。

业务侧需要两种受理模式：

1. **IMMEDIATE 立即到账**：与现状一致，创建后同事务调渠道下单。
2. **APPROVAL 需审核**：创建后进入"待人工审核"状态，运营 / 财务在后台 approve 才真正调渠道下单；reject 则进终态。

两种模式都必须经过现有的金额风控（单笔限额 / 日累计金额 / 日笔数）。

## 2. 设计原则

- payment-service 当前处于 P1 开发期，倾向**简单直接、避免预先抽象**。
- 与 Payment / Refund 聚合的事件命名、状态机风格保持对齐。
- 审核流程下沉到 Transfer 聚合内的扁平字段，不引入独立 Approval 聚合 / ApprovalRecord 子实体（YAGNI）。
- IMMEDIATE 与 APPROVAL 通过 ApplicationService 中的 `submitToGateway()` 复用同一段下渠道逻辑，**零分支**。

## 3. 状态机

```
═══════════ IMMEDIATE 模式 ═══════════
                  ┌─ markSucceeded ──→ SUCCESS ─ markReturned ──→ RETURNED
PENDING ─ submitToChannel ──→ SUBMITTED ─┤
                  └─ markFailed   ──→ FAILED
   │
   └─ 渠道下单同步成功 ────────────────→ (跳过 SUBMITTED) SUCCESS
   └─ 渠道下单同步失败 ────────────────→ FAILED

═══════════ APPROVAL 模式 ═══════════
                            ┌─ approve ─→ (内部 submitToChannel) → SUBMITTED → SUCCESS/FAILED/RETURNED
PENDING_APPROVAL ───────────┤
                            └─ reject  ─→ REJECTED (终态)
```

枚举重组（`TransferStatus`）：

| 旧 | 新 | 说明 |
|---|---|---|
| PENDING | PENDING | IMMEDIATE 模式刚创建,尚未提交渠道 |
| —— | **PENDING_APPROVAL** | APPROVAL 模式等待人工审核 |
| **REVIEWING** | **SUBMITTED** | 已提交渠道,等渠道异步回调（重命名） |
| SUCCESS | SUCCESS | 终态 |
| FAILED | FAILED | 终态 |
| —— | **REJECTED** | 终态:人工拒绝 |
| RETURNED | RETURNED | 终态:渠道退票 |

`isTerminal()` 覆盖 `SUCCESS / FAILED / REJECTED / RETURNED`。

`approve()` 把状态先迁到 `PENDING`，再由同事务调用的 `submitToGateway()` 推进到 `SUBMITTED` / `SUCCESS` / `FAILED`。
外部观察到的迁移是 `PENDING_APPROVAL → SUBMITTED`，`PENDING` 是事务内的瞬态过渡。

## 4. 数据模型

### 4.1 Transfer 实体新增字段

```java
@Enumerated(EnumType.STRING)
@Column(name = "mode", nullable = false, updatable = false, length = 16,
        comment = "受理模式:IMMEDIATE 立即到账 / APPROVAL 需审核")
private TransferMode mode;

@Column(name = "approver_id", length = 64, comment = "审核人用户 ID")
@Nullable private String approverId;

@Column(name = "approved_at", comment = "审核通过时间")
@Nullable private LocalDateTime approvedAt;

@Column(name = "rejected_at", comment = "审核拒绝时间")
@Nullable private LocalDateTime rejectedAt;

@Column(name = "reject_reason", length = 512, comment = "审核拒绝原因")
@Nullable private String rejectReason;
```

`approver_id` 用 `String`（与 `BaseAggregateRoot.createdBy` 同型，跨服务不强类型耦合 sys_user.id）。
`mode` 设 `updatable = false`，防止审核中途切立即到账绕审。

### 4.2 新增枚举 TransferMode

```java
public enum TransferMode { IMMEDIATE, APPROVAL }
```

### 4.3 索引

`@Table` 注解新增：

```
idx_transfer_mode_status (mode, status, create_time)   -- admin 待审列表 / 模式筛选
```

保留：`uk_transfer_biz`、`idx_transfer_status_created`、`idx_transfer_channel_no`、`idx_transfer_recipient`。

### 4.4 Schema 变更方式

依靠 JPA `ddl-auto=update` 自动加列 + 加索引。**不写 Flyway 脚本**（当前为开发阶段）。
对于 `REVIEWING → SUBMITTED` 的旧数据，开发期 `truncate table t_transfer` 重启清库；不写 DML 迁移。

## 5. API 端点

### 5.1 内部接口（`/internal/transfers`，`hasRole('service')`）

| 方法 | 路径 | 变更 |
|---|---|---|
| POST | `/internal/transfers` | `CreateTransferRequest` 增加 `mode` 必填字段（无默认值） |
| GET | `/internal/transfers/{id}` | 无 |
| GET | `/internal/transfers?bizTransferNo=X` | 无 |

`CreateTransferRequest` 新增：

```java
@NotNull
@Schema(description = "受理模式", requiredMode = REQUIRED, example = "APPROVAL")
private TransferMode mode;
```

### 5.2 管理后台接口（`/admin/transfers`，`hasAuthority('payment:transfer:approve')`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/transfers` | 分页查询，支持 `status` / `mode` / `channel` / `bizTransferNo` / `createTimeFrom/To` 过滤 |
| GET | `/admin/transfers/{id}` | 详情 |
| POST | `/admin/transfers/{id}/approve` | 审核通过；Body `{ remark?: string }` |
| POST | `/admin/transfers/{id}/reject` | 审核拒绝；Body `{ reason: string }` 必填 |

约束：
- `/admin/transfers` **不**列入 `eagle.resource-server.permit-paths`，默认需登录
- `approverId` 从 `@AuthenticationPrincipal Jwt.subject` 取，前端不传
- 列表用 `@ParameterObject @PageableDefault Pageable` + `TransferAdminQueryRequest`（rule 18）
- approve / reject 走 `@AuditLog`

新增 DTO：
- `ApproveTransferRequest { @Nullable @Size(max=512) String remark }`
- `RejectTransferRequest { @NotBlank @Size(max=512) String reason }`
- `TransferAdminQueryRequest { TransferStatus status; TransferMode mode; PaymentChannel channel; String bizTransferNo; LocalDateTime createTimeFrom; LocalDateTime createTimeTo; }`

## 6. ApplicationService 编排

### 6.1 修改 `create()`

```java
@Transactional
public Transfer create(CreateTransferRequest request) {
    if (!properties.getTransfer().isEnabled()) throw TRANSFER_DISABLED;
    checkRiskControl(request.getAmount());                  // 两种模式都跑
    ensureNotDuplicate(request.getBizTransferNo());
    PaymentGatewayPort gateway = resolveGateway(request.getChannel());

    Transfer transfer = Transfer.create(
            request.getBizTransferNo(), request.getMode(), request.getChannel(),
            request.getRecipientAccount(), request.getRecipientName(),
            request.getAmount(), request.getReason());
    transfer = saveAndCatchUnique(transfer);

    if (request.getMode() == TransferMode.APPROVAL) {
        return transfer;                                    // PENDING_APPROVAL,不调渠道
    }
    return submitToGateway(transfer, gateway);              // 现有逻辑抽方法
}
```

### 6.2 新增 `approve()` / `reject()`

```java
@Transactional
public Transfer approve(Long transferId, String approverId, @Nullable String remark) {
    Transfer t = findByIdForUpdate(transferId);             // 悲观锁防双审
    t.approve(approverId);                                  // 聚合根方法迁状态
    PaymentGatewayPort gw = gateways.get(t.getChannel());
    if (gw == null) throw CHANNEL_UNAVAILABLE;
    return submitToGateway(t, gw);                          // 同事务下渠道
}

@Transactional
public Transfer reject(Long transferId, String approverId, String reason) {
    Transfer t = findByIdForUpdate(transferId);
    t.reject(approverId, reason);
    return transferRepository.save(t);
}
```

### 6.3 Transfer 聚合根新增方法

```java
public void approve(String approverId) {
    if (mode != TransferMode.APPROVAL) throw NOT_APPROVAL_MODE;
    if (status != TransferStatus.PENDING_APPROVAL) throw APPROVAL_NOT_ALLOWED_IN_STATUS;
    this.approverId = approverId;
    this.approvedAt = LocalDateTime.now();
    this.status = TransferStatus.PENDING;                   // 内部过渡态
    registerEvent(new TransferApprovedEvent(getId(), bizTransferNo, channel,
            amount, recipientAccount, approverId, this.approvedAt));
}

public void reject(String approverId, String reason) {
    if (status != TransferStatus.PENDING_APPROVAL) throw APPROVAL_NOT_ALLOWED_IN_STATUS;
    this.approverId = approverId;
    this.rejectedAt = LocalDateTime.now();
    this.rejectReason = reason;
    this.status = TransferStatus.REJECTED;
    registerEvent(new TransferRejectedEvent(getId(), bizTransferNo, channel,
            amount, recipientAccount, approverId, reason, this.rejectedAt));
}
```

### 6.4 仓库方法

`TransferRepository` 新增：

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Transfer t WHERE t.id = :id")
Optional<Transfer> findByIdForUpdate(@Param("id") Long id);

Page<Transfer> findAll(Specification<Transfer> spec, Pageable pageable);   // admin 列表
```

## 7. 集成事件

### 7.1 领域事件（`domain/event/`）

```java
public record TransferApprovedEvent(
    String eventId, LocalDateTime occurredOn,
    Long transferId, String bizTransferNo, PaymentChannel channel,
    BigDecimal amount, String recipientAccount,
    String approverId, LocalDateTime approvedAt
) implements BaseEvent { }

public record TransferRejectedEvent(
    String eventId, LocalDateTime occurredOn,
    Long transferId, String bizTransferNo, PaymentChannel channel,
    BigDecimal amount, String recipientAccount,
    String approverId, String rejectReason, LocalDateTime rejectedAt
) implements BaseEvent { }
```

### 7.2 集成事件（`infrastructure/messaging/`）

- `TransferApprovedIntegrationEvent` — topic `payment_transfer_events`，tag `approved`
- `TransferRejectedIntegrationEvent` — 同 topic，tag `rejected`

**Topic 改名**：现有 `transfer_*` 全部统一为 `payment_transfer_events`，tag 集为
`submitted / succeeded / failed / returned / approved / rejected`，与 Payment / Refund 风格一致。
P1 开发期改名成本低。

### 7.3 事件 schema 文档

`docs/events/payment_transfer_events.md` 同步维护字段表 + 版本 + 已知消费方清单（参考 rule 15）。

## 8. 错误码

`TransferErrorCode` 续号：

| 编码 | 常量 | i18n key | 中文 |
|---|---|---|---|
| 70050 | `TRANSFER_MODE_REQUIRED` | `error.transfer.mode_required` | 受理模式不能为空 |
| 70051 | `APPROVAL_NOT_ALLOWED_IN_STATUS` | `error.transfer.approval_not_allowed` | 当前状态不允许审核操作 |
| 70052 | `REJECT_REASON_REQUIRED` | `error.transfer.reject_reason_required` | 拒绝原因不能为空 |
| 70053 | `NOT_APPROVAL_MODE` | `error.transfer.not_approval_mode` | 立即到账模式不支持审核操作 |

三语 i18n（zh_CN / en_US / zh_TW）同步增加。

## 9. 配置

不新增配置项。风控阈值复用现有 `eagle.payment.transfer.single-amount-limit` / `daily-amount-limit` / `daily-count-limit`。

## 10. 测试策略

无基础设施依赖（rule 09）：JUnit 5 + Mockito，不连 H2 / Redis / RocketMQ / Nacos。

| 测试类 | 覆盖 |
|---|---|
| `TransferTest` 扩展 | mode 必填、`approve()` / `reject()` 状态机迁移、非法状态抛错、事件注册 |
| `TransferApplicationServiceTest` 扩展 | IMMEDIATE 走渠道、APPROVAL 仅持久化、`approve()` 同事务调渠道、`reject()` 不调渠道、风控双模式生效、并发审核（mock forUpdate）、approve 渠道失败事务回滚 |
| `TransferAdminControllerTest`（新增 MockMvc 切片） | approve / reject 权限、reject 缺 reason 400、approverId 从 JWT 取 |
| `TransferIntegrationEventPublisherTest` 扩展 | `onApproved` / `onRejected` 发布到正确 topic + tag |

架构测试：本次不动模块边界，`ModulithArchitectureTest` 现状即可。

## 11. 不在本设计范围

- 多级审核（当前单级，结构可平滑演进到 `ApprovalRecord` 子实体）
- 审核超时自动拒绝（用户决定不超时）
- 审核者粒度细到部门 / 数据权限（`@DataPermission` 暂不接入）
- 资金账户冻结（Transfer 不管账户余额，调用方在 payment-service 之上自行处理）
- recipient_account/name 加密（保留 P1 hook 不动）

## 12. 风险与回滚

| 风险 | 缓解 |
|---|---|
| `mode` 字段在 `CreateTransferRequest` 必填 → 已对接的上游调用方报 400 | payment-service P1 开发期，上游尚未集成；变更与设计同步通知 |
| `REVIEWING → SUBMITTED` 重命名导致历史数据语义错乱 | 开发期清库重建 |
| topic `transfer_*` 改 `payment_transfer_events` 破坏 consumer | 当前无 consumer 接入 |
| `ddl-auto=update` 给已有数据的表加 NOT NULL 列失败 | 开发期清库 `truncate table t_transfer` 后再启动；与 §4.4 重启清库一致 |

回滚：本次为新增 + 重命名，回滚成本主要在删字段 / 改回 enum 值，开发期可清库重建。

---

下一步：按本设计运行 `superpowers:writing-plans` 产出实施计划。

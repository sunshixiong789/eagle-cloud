# Transfer 审核模式 + 立即到账双受理路径 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `eagle-payment-service` 的 Transfer 聚合新增「需要审核 (APPROVAL)」和「立即到账 (IMMEDIATE)」两种受理模式，APPROVAL 路径通过新的 `/admin/transfers/**` 端点完成 approve/reject 后再调渠道。

**Architecture:** 聚合内扁平字段方案 —— Transfer 聚合根新增 `mode` + 4 个审核相关字段；新增 `PENDING_APPROVAL` / `REJECTED` 两个状态；现有 `REVIEWING` 重命名为 `SUBMITTED`；`approve()` 同事务复用现有 `submitToGateway()` 逻辑（零分支）；`/admin/transfers/**` 新增 admin controller 走 `hasAuthority('payment:transfer:approve')`。

**Tech Stack:** Java 25 / Spring Boot 4.0.6 / Spring Modulith / JPA / Hibernate / JUnit 5 / Mockito / Spring Security OAuth2 Resource Server / RocketMQ（eagle-rocketmq-starter）。开发期使用 JPA `ddl-auto=update`，不写 Flyway。

**设计稿：** `docs/superpowers/specs/2026-06-08-transfer-approval-mode-design.md`

**与设计稿的偏差修正（基于现有代码现状）：**
- Topic 已是 `payment_transfer_events`（无需改名），仅新增 `approved` / `rejected` 两个 tag；现有 tag `success` / `failed` / `returned` 保留不动。
- 现有领域事件是裸 `record`（如 `TransferSucceededEvent`），新事件按现状风格走，不继承 `BaseEvent`。
- i18n 文件只有 `messages_zh_CN.properties` + `messages_en.properties`（无 zh_TW），同步这两个。

**模块根路径：** `/Users/sunshixiong/IdeaProjects/eagle-cloud/eagle-services/eagle-payment-service`
**源码根：** `src/main/java/com/eagle/payment/core/`
**测试根：** `src/test/java/com/eagle/payment/core/`

**PR 前必跑：**

```bash
./gradlew :eagle-services:eagle-payment-service:test
./gradlew clean build
```

**Commit 风格：** `<type>(payment): <subject>`，常用 type: `feat / refactor / test / docs`，subject 中文 / 英文均可（与现有 git log 一致）。

---

## File Structure

**新建文件**（13 个）：

```
src/main/java/com/eagle/payment/core/
├── domain/
│   ├── model/
│   │   └── enums/
│   │       └── TransferMode.java                      # 新建:枚举 IMMEDIATE / APPROVAL
│   └── event/
│       ├── TransferApprovedEvent.java                 # 新建:领域事件 record
│       └── TransferRejectedEvent.java                 # 新建:领域事件 record
├── interfaces/
│   ├── controller/
│   │   └── TransferAdminController.java               # 新建:/admin/transfers/**
│   └── dto/
│       ├── request/
│       │   ├── ApproveTransferRequest.java            # 新建:approve body
│       │   ├── RejectTransferRequest.java             # 新建:reject body
│       │   └── TransferAdminQueryRequest.java         # 新建:列表查询参数
└── infrastructure/
    └── messaging/
        ├── TransferApprovedIntegrationEvent.java      # 新建:集成事件
        └── TransferRejectedIntegrationEvent.java      # 新建:集成事件

src/test/java/com/eagle/payment/core/
└── interfaces/
    └── controller/
        └── TransferAdminControllerTest.java           # 新建:MockMvc 切片测试
```

**修改文件**（11 个）：

```
src/main/java/com/eagle/payment/core/
├── domain/
│   ├── model/
│   │   ├── enums/
│   │   │   └── TransferStatus.java                    # 重组:REVIEWING→SUBMITTED + 加 PENDING_APPROVAL/REJECTED
│   │   └── aggregate/
│   │       └── Transfer.java                          # 加 mode + 审核字段 + approve/reject 方法
│   └── repository/
│       └── TransferRepository.java                    # 加 findByIdForUpdate + Specification 查询
├── application/
│   ├── service/
│   │   └── TransferApplicationService.java            # create() 加 mode 分支 + 新增 approve/reject
│   └── mapper/
│       └── TransferMapper.java                        # 输出新字段
├── interfaces/
│   └── dto/
│       ├── request/
│       │   └── CreateTransferRequest.java             # 加 mode 必填字段
│       └── response/
│           └── TransferResponse.java                  # 加新字段
└── common/
    └── exception/
        └── TransferErrorCode.java                     # 加 70050-70053

src/main/java/com/eagle/payment/core/infrastructure/event/
└── TransferIntegrationEventPublisher.java             # 加 onApproved/onRejected

src/main/resources/
├── messages_zh_CN.properties                          # 4 个新 key
└── messages_en.properties                             # 4 个新 key

src/test/java/com/eagle/payment/core/
├── domain/model/aggregate/TransferTest.java           # 扩展 approve/reject 测试
└── application/service/TransferApplicationServiceTest.java   # 扩展 mode 分支 + admin 操作测试
```

---

## Task 1: 新增 TransferMode 枚举

**Files:**
- Create: `src/main/java/com/eagle/payment/core/domain/model/enums/TransferMode.java`

- [ ] **Step 1: 创建 TransferMode 枚举**

写入：

```java
package com.eagle.payment.core.domain.model.enums;

/**
 * Transfer 受理模式。
 *
 * <ul>
 *   <li>{@link #IMMEDIATE} 立即到账:create 后直接调渠道下单。</li>
 *   <li>{@link #APPROVAL} 需审核:create 后进入 {@code PENDING_APPROVAL},
 *       由管理员通过 {@code /admin/transfers/{id}/approve} 审核后再调渠道。</li>
 * </ul>
 *
 * @author sunshixiong
 */
public enum TransferMode {
    IMMEDIATE,
    APPROVAL
}
```

- [ ] **Step 2: 编译确认无错**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/model/enums/TransferMode.java
git commit -m "feat(payment): 新增 TransferMode 枚举 IMMEDIATE/APPROVAL"
```

---

## Task 2: TransferStatus 重组（REVIEWING→SUBMITTED + 新增 PENDING_APPROVAL/REJECTED）

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/domain/model/enums/TransferStatus.java`
- Modify: `src/main/java/com/eagle/payment/core/domain/model/aggregate/Transfer.java` (引用更新)
- Modify: `src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java` (ACCOUNTED_STATUSES)
- Modify: `src/test/java/com/eagle/payment/core/domain/model/aggregate/TransferTest.java` (REVIEWING 引用)
- Modify: `src/test/java/com/eagle/payment/core/application/service/TransferApplicationServiceTest.java` (REVIEWING 引用)

- [ ] **Step 1: 重写 TransferStatus**

替换 `TransferStatus.java` 整文件为：

```java
package com.eagle.payment.core.domain.model.enums;

/**
 * 提现 / B2C 转账状态机。
 *
 * <pre>
 *   IMMEDIATE 模式:
 *     PENDING ──submittedToChannel──&gt; SUBMITTED ──成功──&gt; SUCCESS ──退票──&gt; RETURNED
 *                                              ──失败──&gt; FAILED
 *     PENDING ──渠道下单失败────────&gt; FAILED
 *
 *   APPROVAL 模式:
 *     PENDING_APPROVAL ──approve──&gt; (内部 submittedToChannel) → SUBMITTED → SUCCESS/FAILED/RETURNED
 *     PENDING_APPROVAL ──reject ──&gt; REJECTED
 * </pre>
 *
 * <p>终态:{@link #SUCCESS} / {@link #FAILED} / {@link #REJECTED} / {@link #RETURNED}。
 *
 * @author sunshixiong
 */
public enum TransferStatus {
    PENDING,
    PENDING_APPROVAL,
    SUBMITTED,
    SUCCESS,
    FAILED,
    REJECTED,
    RETURNED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == REJECTED || this == RETURNED;
    }
}
```

- [ ] **Step 2: 全仓搜索 REVIEWING 引用,替换为 SUBMITTED**

Run:
```bash
grep -rn "TransferStatus.REVIEWING\|TransferStatus\.REVIEWING" eagle-services/eagle-payment-service/src
```

Expected: 列出所有引用位置。

逐一替换：
- `Transfer.java`：`submittedToChannel()` 中 `this.status = TransferStatus.REVIEWING` → `this.status = TransferStatus.SUBMITTED`；`markSucceeded()` / `markFailed()` 中 `status != TransferStatus.REVIEWING` → `status != TransferStatus.SUBMITTED`
- `TransferApplicationService.java`：`ACCOUNTED_STATUSES = List.of(TransferStatus.REVIEWING, TransferStatus.SUCCESS)` → `List.of(TransferStatus.SUBMITTED, TransferStatus.SUCCESS)`
- `TransferTest.java` / `TransferApplicationServiceTest.java`：所有 `TransferStatus.REVIEWING` → `TransferStatus.SUBMITTED`
- `TransferRepository.java` Javadoc `按状态 IN (REVIEWING, SUCCESS) 汇总` → `按状态 IN (SUBMITTED, SUCCESS) 汇总`

- [ ] **Step 3: 跑测试确认重命名不破坏现有行为**

Run: `./gradlew :eagle-services:eagle-payment-service:test`
Expected: BUILD SUCCESSFUL，现有测试全部通过。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/model/enums/TransferStatus.java \
  src/main/java/com/eagle/payment/core/domain/model/aggregate/Transfer.java \
  src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java \
  src/main/java/com/eagle/payment/core/domain/repository/TransferRepository.java \
  src/test/java/com/eagle/payment/core/domain/model/aggregate/TransferTest.java \
  src/test/java/com/eagle/payment/core/application/service/TransferApplicationServiceTest.java
git commit -m "refactor(payment): TransferStatus 重命名 REVIEWING→SUBMITTED + 新增 PENDING_APPROVAL/REJECTED"
```

---

## Task 3: Transfer 实体加 mode + 审核字段 + create() 接受 mode

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/domain/model/aggregate/Transfer.java`
- Modify: `src/test/java/com/eagle/payment/core/domain/model/aggregate/TransferTest.java`

- [ ] **Step 1: 在 TransferTest 增加 mode 分支测试（先失败）**

在 `TransferTest.Create` 内类追加：

```java
        @Test
        @DisplayName("IMMEDIATE 模式初始状态应为 PENDING")
        void shouldStartAtPendingForImmediate() {
            Transfer t = Transfer.create("TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "月度结算");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING);
            assertThat(t.getMode()).isEqualTo(TransferMode.IMMEDIATE);
        }

        @Test
        @DisplayName("APPROVAL 模式初始状态应为 PENDING_APPROVAL")
        void shouldStartAtPendingApprovalForApproval() {
            Transfer t = Transfer.create("TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "月度结算");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
            assertThat(t.getMode()).isEqualTo(TransferMode.APPROVAL);
        }
```

同时把 TransferTest 顶部 `create()` 辅助方法改为：

```java
    private Transfer create() {
        return Transfer.create("TRN-001", TransferMode.IMMEDIATE, PaymentChannel.ALIPAY,
                "user@example.com", "张三", new BigDecimal("500.00"), "月度结算");
    }
```

import 增加：`import com.eagle.payment.core.domain.model.enums.TransferMode;`

- [ ] **Step 2: 跑测试验证失败（因为 Transfer.create 签名还没变）**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.domain.model.aggregate.TransferTest"`
Expected: 编译失败（`create` 方法签名不匹配）

- [ ] **Step 3: 修改 Transfer.java 加字段与签名**

在 Transfer 类字段区（`amount` 字段下方、`reason` 字段前后位置）加入：

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 16,
            comment = "受理模式:IMMEDIATE 立即到账 / APPROVAL 需审核")
    private TransferMode mode;

    @Column(name = "approver_id", length = 64, comment = "审核人用户 ID")
    @Nullable
    private String approverId;

    @Column(name = "approved_at", comment = "审核通过时间")
    @Nullable
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at", comment = "审核拒绝时间")
    @Nullable
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason", length = 512, comment = "审核拒绝原因")
    @Nullable
    private String rejectReason;
```

import 增加：`import com.eagle.payment.core.domain.model.enums.TransferMode;`

`@Table` 注解的 `indexes` 增加一项（在现有 3 个 index 之后）：

```java
                @Index(name = "idx_transfer_mode_status",
                        columnList = "mode, status, create_time")
```

修改私有构造器与 `create` 静态工厂方法：

```java
    private Transfer(String bizTransferNo, TransferMode mode, PaymentChannel channel,
                     String recipientAccount, @Nullable String recipientName,
                     BigDecimal amount, @Nullable String reason) {
        this.bizTransferNo = bizTransferNo;
        this.mode = mode;
        this.channel = channel;
        this.recipientAccount = recipientAccount;
        this.recipientName = recipientName;
        this.amount = amount;
        this.reason = reason;
        this.status = (mode == TransferMode.APPROVAL)
                ? TransferStatus.PENDING_APPROVAL
                : TransferStatus.PENDING;
    }

    public static Transfer create(String bizTransferNo, TransferMode mode,
                                  PaymentChannel channel,
                                  String recipientAccount, @Nullable String recipientName,
                                  BigDecimal amount, @Nullable String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw TransferErrorCode.INVALID_TRANSFER_AMOUNT.toDomainException();
        }
        return new Transfer(bizTransferNo, mode, channel, recipientAccount, recipientName,
                amount.setScale(2), reason);
    }
```

- [ ] **Step 4: 修复 TransferApplicationService 调用点（仍只调 IMMEDIATE，暂时硬编码）**

定位 `TransferApplicationService.create()` 里的 `Transfer.create(...)` 调用，改为：

```java
        Transfer transfer = Transfer.create(request.getBizTransferNo(),
                TransferMode.IMMEDIATE,
                request.getChannel(), request.getRecipientAccount(),
                request.getRecipientName(), request.getAmount(), request.getReason());
```

import 增加：`import com.eagle.payment.core.domain.model.enums.TransferMode;`

（Task 8 会读取 request.getMode() 替换硬编码 IMMEDIATE。）

- [ ] **Step 5: 跑测试验证通过**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.domain.model.aggregate.TransferTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/model/aggregate/Transfer.java \
  src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java \
  src/test/java/com/eagle/payment/core/domain/model/aggregate/TransferTest.java
git commit -m "feat(payment): Transfer 加 mode + 审核字段 + create() 接收 mode"
```

---

## Task 4: 新增 TransferApprovedEvent / TransferRejectedEvent 领域事件

**Files:**
- Create: `src/main/java/com/eagle/payment/core/domain/event/TransferApprovedEvent.java`
- Create: `src/main/java/com/eagle/payment/core/domain/event/TransferRejectedEvent.java`

- [ ] **Step 1: 创建 TransferApprovedEvent**

```java
package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核通过领域事件。
 *
 * @author sunshixiong
 */
public record TransferApprovedEvent(
        Long transferId,
        String bizTransferNo,
        PaymentChannel channel,
        BigDecimal amount,
        String recipientAccount,
        String approverId,
        LocalDateTime approvedAt
) {
}
```

- [ ] **Step 2: 创建 TransferRejectedEvent**

```java
package com.eagle.payment.core.domain.event;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核拒绝领域事件。
 *
 * @author sunshixiong
 */
public record TransferRejectedEvent(
        Long transferId,
        String bizTransferNo,
        PaymentChannel channel,
        BigDecimal amount,
        String recipientAccount,
        String approverId,
        String rejectReason,
        LocalDateTime rejectedAt
) {
}
```

- [ ] **Step 3: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/event/TransferApprovedEvent.java \
  src/main/java/com/eagle/payment/core/domain/event/TransferRejectedEvent.java
git commit -m "feat(payment): 新增 TransferApprovedEvent / TransferRejectedEvent 领域事件"
```

---

## Task 5: TransferErrorCode 新增 4 个错误码 + i18n

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/common/exception/TransferErrorCode.java`
- Modify: `src/main/resources/messages_zh_CN.properties`
- Modify: `src/main/resources/messages_en.properties`

- [ ] **Step 1: TransferErrorCode 加 4 个枚举**

在最后一个常量 `CHANNEL_UNAVAILABLE(70049, ...)` 后追加（注意把它的分号改为逗号）：

```java
    TRANSFER_MODE_REQUIRED(70050, "error.transfer.mode_required", "受理模式不能为空"),
    APPROVAL_NOT_ALLOWED_IN_STATUS(70051, "error.transfer.approval_not_allowed",
            "当前状态不允许审核操作"),
    REJECT_REASON_REQUIRED(70052, "error.transfer.reject_reason_required",
            "拒绝原因不能为空"),
    NOT_APPROVAL_MODE(70053, "error.transfer.not_approval_mode",
            "立即到账模式不支持审核操作");
```

- [ ] **Step 2: messages_zh_CN.properties 加 4 行**

在 `error.transfer.channel_unavailable` 行之后追加：

```properties
error.transfer.mode_required            = 受理模式不能为空
error.transfer.approval_not_allowed     = 当前状态不允许审核操作
error.transfer.reject_reason_required   = 拒绝原因不能为空
error.transfer.not_approval_mode        = 立即到账模式不支持审核操作
```

- [ ] **Step 3: messages_en.properties 加 4 行**

```properties
error.transfer.mode_required            = Transfer mode is required
error.transfer.approval_not_allowed     = Current status does not allow approval action
error.transfer.reject_reason_required   = Reject reason is required
error.transfer.not_approval_mode        = Immediate transfer mode does not support approval actions
```

- [ ] **Step 4: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eagle/payment/core/common/exception/TransferErrorCode.java \
  src/main/resources/messages_zh_CN.properties \
  src/main/resources/messages_en.properties
git commit -m "feat(payment): TransferErrorCode 新增 70050-70053 + i18n 双语"
```

---

## Task 6: Transfer.approve() / Transfer.reject() 聚合根方法

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/domain/model/aggregate/Transfer.java`
- Modify: `src/test/java/com/eagle/payment/core/domain/model/aggregate/TransferTest.java`

- [ ] **Step 1: 在 TransferTest 增加 approve / reject 测试组**

本步只验证聚合状态机迁移与字段，事件注册的验证放到 ApplicationService 测试（通过 Mock 仓库捕获 + Spring `ApplicationEvents` 或 mock `EventPublisher`）；TransferTest 中不直接断言事件列表，避免与 `BaseAggregateRoot` 内部存储实现耦合。

在 `TransferTest` 类内追加 `Approve` / `Reject` 两个 `@Nested` 组：

```java
    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("APPROVAL 模式 PENDING_APPROVAL → PENDING + 记录 approverId/approvedAt")
        void shouldTransitionToPendingAndRecordApprover() {
            Transfer t = Transfer.create("TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            t.approve("admin-1");

            assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING);
            assertThat(t.getApproverId()).isEqualTo("admin-1");
            assertThat(t.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("IMMEDIATE 模式 approve 抛 NOT_APPROVAL_MODE")
        void shouldRejectApproveOnImmediate() {
            Transfer t = Transfer.create("TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            assertThatThrownBy(() -> t.approve("admin-1"))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("非 PENDING_APPROVAL 状态 approve 抛 APPROVAL_NOT_ALLOWED_IN_STATUS")
        void shouldRejectApproveWhenStatusNotPendingApproval() {
            Transfer t = Transfer.create("TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");
            t.approve("admin-1");

            assertThatThrownBy(() -> t.approve("admin-2"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("APPROVAL 模式 PENDING_APPROVAL → REJECTED + 记录原因")
        void shouldTransitionToRejected() {
            Transfer t = Transfer.create("TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            t.reject("admin-1", "金额可疑");

            assertThat(t.getStatus()).isEqualTo(TransferStatus.REJECTED);
            assertThat(t.getApproverId()).isEqualTo("admin-1");
            assertThat(t.getRejectReason()).isEqualTo("金额可疑");
            assertThat(t.getRejectedAt()).isNotNull();
        }

        @Test
        @DisplayName("非 PENDING_APPROVAL 状态 reject 抛 APPROVAL_NOT_ALLOWED_IN_STATUS")
        void shouldRejectRejectWhenStatusNotPendingApproval() {
            Transfer t = Transfer.create("TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            assertThatThrownBy(() -> t.reject("admin-1", "any"))
                    .isInstanceOf(DomainException.class);
        }
    }
```

- [ ] **Step 2: 跑测试确认失败（方法不存在）**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.domain.model.aggregate.TransferTest"`
Expected: 编译失败 `approve / reject 不存在`

- [ ] **Step 3: 在 Transfer 加 approve() 方法**

在 `markReturned()` 之后追加：

```java
    /**
     * 审核通过:仅允许从 APPROVAL 模式 + PENDING_APPROVAL 状态迁出,
     * 状态先迁到 PENDING (内部过渡态,事务内由 submitToGateway 继续推进)。
     */
    public void approve(String approverId) {
        if (this.mode != TransferMode.APPROVAL) {
            throw TransferErrorCode.NOT_APPROVAL_MODE.toDomainException();
        }
        if (this.status != TransferStatus.PENDING_APPROVAL) {
            throw TransferErrorCode.APPROVAL_NOT_ALLOWED_IN_STATUS.toDomainException();
        }
        this.status = TransferStatus.PENDING;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
        registerEvent(new TransferApprovedEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, approverId, this.approvedAt));
    }

    /**
     * 审核拒绝:仅允许从 PENDING_APPROVAL 迁出 → REJECTED 终态。
     */
    public void reject(String approverId, String reason) {
        if (this.status != TransferStatus.PENDING_APPROVAL) {
            throw TransferErrorCode.APPROVAL_NOT_ALLOWED_IN_STATUS.toDomainException();
        }
        this.status = TransferStatus.REJECTED;
        this.approverId = approverId;
        this.rejectReason = reason;
        this.rejectedAt = LocalDateTime.now();
        registerEvent(new TransferRejectedEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, approverId, reason, this.rejectedAt));
    }
```

import 增加：

```java
import com.eagle.payment.core.domain.event.TransferApprovedEvent;
import com.eagle.payment.core.domain.event.TransferRejectedEvent;
```

- [ ] **Step 4: 跑测试验证通过**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.domain.model.aggregate.TransferTest"`
Expected: BUILD SUCCESSFUL，所有 approve / reject 测试通过

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/model/aggregate/Transfer.java \
  src/test/java/com/eagle/payment/core/domain/model/aggregate/TransferTest.java
git commit -m "feat(payment): Transfer 加 approve/reject 聚合根方法 + 测试"
```

---

## Task 7: CreateTransferRequest 加 mode 必填字段

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/interfaces/dto/request/CreateTransferRequest.java`

- [ ] **Step 1: 在 channel 字段之后追加 mode 字段**

```java
    @NotNull
    @Schema(description = "受理模式:IMMEDIATE 立即到账 / APPROVAL 需审核",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVAL")
    private TransferMode mode;
```

import 增加：`import com.eagle.payment.core.domain.model.enums.TransferMode;`

- [ ] **Step 2: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/eagle/payment/core/interfaces/dto/request/CreateTransferRequest.java
git commit -m "feat(payment): CreateTransferRequest 加 mode 必填字段"
```

---

## Task 8: TransferApplicationService.create() 按 mode 分支

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java`
- Modify: `src/test/java/com/eagle/payment/core/application/service/TransferApplicationServiceTest.java`

- [ ] **Step 1: 抽 submitToGateway 私有方法**

把 `create()` 中"提交渠道 + 处理结果"那段抽出来。重写 `create()` + 新增 `submitToGateway()`：

```java
    @Transactional
    public Transfer create(CreateTransferRequest request) {
        if (!properties.getTransfer().isEnabled()) {
            throw TransferErrorCode.TRANSFER_DISABLED.toDomainException();
        }
        checkRiskControl(request.getAmount());

        if (transferRepository.existsByBizTransferNo(request.getBizTransferNo())) {
            throw TransferErrorCode.DUPLICATE_TRANSFER.toConflictException();
        }
        PaymentGatewayPort gateway = gateways.get(request.getChannel());
        if (gateway == null) {
            throw TransferErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
        Transfer transfer = Transfer.create(request.getBizTransferNo(),
                request.getMode(), request.getChannel(),
                request.getRecipientAccount(), request.getRecipientName(),
                request.getAmount(), request.getReason());
        try {
            transfer = transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException e) {
            if (transferRepository.existsByBizTransferNo(request.getBizTransferNo())) {
                throw TransferErrorCode.DUPLICATE_TRANSFER.toConflictException();
            }
            throw e;
        }

        if (request.getMode() == TransferMode.APPROVAL) {
            log.info("transfer created (awaiting approval), id={}, channel={}, status={}",
                    transfer.getId(), request.getChannel(), transfer.getStatus());
            return transfer;
        }
        return submitToGateway(transfer, gateway);
    }

    /**
     * 把 PENDING 状态 transfer 推送到渠道,并按渠道返回结果更新状态。
     */
    private Transfer submitToGateway(Transfer transfer, PaymentGatewayPort gateway) {
        GatewayTransferResult result = gateway.transfer(new GatewayTransferCommand(
                transfer.getChannel(),
                transfer.getBizTransferNo(),
                transfer.getAmount(),
                "CNY",
                transfer.getRecipientAccount(),
                transfer.getRecipientName(),
                transfer.getReason()
        ));
        if (result.status() == TransferStatus.SUCCESS) {
            transfer.submittedToChannel(result.channelTransferNo());
            transfer.markSucceeded(
                    result.succeededAt() != null ? result.succeededAt() : LocalDateTime.now(),
                    result.channelTransferNo());
        } else if (result.status() == TransferStatus.FAILED) {
            transfer.markFailed(result.failReason());
        } else {
            transfer.submittedToChannel(result.channelTransferNo());
        }
        Transfer saved = transferRepository.save(transfer);
        log.info("transfer submitted to gateway, id={}, channel={}, status={}, channelTransferNo={}",
                saved.getId(), saved.getChannel(), saved.getStatus(),
                saved.getChannelTransferNo());
        return saved;
    }
```

import 增加：`import com.eagle.payment.core.domain.model.enums.TransferMode;`

- [ ] **Step 2: 修复 TransferApplicationServiceTest 工厂方法**

在 `TransferApplicationServiceTest.request(...)` 中追加 `req.setMode(TransferMode.IMMEDIATE);`，让现有所有测试默认走 IMMEDIATE：

```java
    private CreateTransferRequest request(BigDecimal amount) {
        CreateTransferRequest req = new CreateTransferRequest();
        req.setBizTransferNo("TRN-001");
        req.setMode(TransferMode.IMMEDIATE);
        req.setChannel(PaymentChannel.ALIPAY);
        req.setRecipientAccount("user@example.com");
        req.setRecipientName("张三");
        req.setAmount(amount);
        req.setReason("月度结算");
        return req;
    }
```

import 增加：`import com.eagle.payment.core.domain.model.enums.TransferMode;`

- [ ] **Step 3: 增加 APPROVAL 分支测试**

在 `TransferApplicationServiceTest.Create` 类内追加：

```java
        @Test
        @DisplayName("APPROVAL 模式 create 应停在 PENDING_APPROVAL,不调渠道")
        void shouldStopAtPendingApprovalForApprovalMode() {
            stubRiskControlOk();
            when(transferRepository.existsByBizTransferNo(eq("TRN-001"))).thenReturn(false);
            when(transferRepository.saveAndFlush(any(Transfer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CreateTransferRequest req = request(new BigDecimal("500.00"));
            req.setMode(TransferMode.APPROVAL);

            Transfer result = service.create(req);

            assertThat(result.getStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
            assertThat(result.getMode()).isEqualTo(TransferMode.APPROVAL);
            verify(alipayGateway, never()).transfer(any());
        }
```

- [ ] **Step 4: 跑测试**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.application.service.TransferApplicationServiceTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java \
  src/test/java/com/eagle/payment/core/application/service/TransferApplicationServiceTest.java
git commit -m "feat(payment): TransferApplicationService.create 按 mode 分支 + 抽 submitToGateway"
```

---

## Task 9: Repository.findByIdForUpdate + ApplicationService.approve/reject

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/domain/repository/TransferRepository.java`
- Modify: `src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java`
- Modify: `src/test/java/com/eagle/payment/core/application/service/TransferApplicationServiceTest.java`

- [ ] **Step 1: TransferRepository 加 findByIdForUpdate**

在现有 `findByIdForUpdate` 不存在位置（与其它 `Optional<Transfer> findBy...` 同区域）追加：

```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transfer t WHERE t.id = :id")
    Optional<Transfer> findByIdForUpdate(@Param("id") Long id);
```

imports 增加：

```java
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
```

- [ ] **Step 2: ApplicationService 增加 approve / reject 方法**

在 `findByBizTransferNo()` 之前追加：

```java
    @Transactional
    public Transfer approve(Long transferId, String approverId, @Nullable String remark) {
        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(TransferErrorCode.TRANSFER_NOT_FOUND::toNotFoundException);
        transfer.approve(approverId);
        PaymentGatewayPort gateway = gateways.get(transfer.getChannel());
        if (gateway == null) {
            throw TransferErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
        Transfer result = submitToGateway(transfer, gateway);
        log.info("transfer approved, id={}, approverId={}, remark={}, finalStatus={}",
                result.getId(), approverId, remark, result.getStatus());
        return result;
    }

    @Transactional
    public Transfer reject(Long transferId, String approverId, String reason) {
        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(TransferErrorCode.TRANSFER_NOT_FOUND::toNotFoundException);
        transfer.reject(approverId, reason);
        Transfer saved = transferRepository.save(transfer);
        log.info("transfer rejected, id={}, approverId={}, reason={}",
                saved.getId(), approverId, reason);
        return saved;
    }
```

imports 增加：`import org.jspecify.annotations.Nullable;`

- [ ] **Step 3: 在测试中加 Approve / Reject 类**

在 `TransferApplicationServiceTest` 内追加：

```java
    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("APPROVAL 模式 PENDING_APPROVAL → 同事务调渠道并到达 SUCCESS")
        void shouldApproveAndSubmitToGateway() {
            Transfer pending = Transfer.create("TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");
            when(transferRepository.findByIdForUpdate(eq(1L))).thenReturn(java.util.Optional.of(pending));
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
            GatewayTransferResult result = new GatewayTransferResult(
                    TransferStatus.SUCCESS, "CH-TRN-001",
                    LocalDateTime.now(), null);
            when(alipayGateway.transfer(any(GatewayTransferCommand.class))).thenReturn(result);

            Transfer out = service.approve(1L, "admin-1", "ok");

            assertThat(out.getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(out.getApproverId()).isEqualTo("admin-1");
            verify(alipayGateway).transfer(any(GatewayTransferCommand.class));
        }

        @Test
        @DisplayName("transfer 不存在抛 TRANSFER_NOT_FOUND")
        void shouldThrowWhenTransferNotFound() {
            when(transferRepository.findByIdForUpdate(eq(1L))).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.approve(1L, "admin-1", null))
                    .isInstanceOf(com.eagle.common.exception.NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("APPROVAL 模式 PENDING_APPROVAL → REJECTED,不调渠道")
        void shouldRejectAndNotCallGateway() {
            Transfer pending = Transfer.create("TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");
            when(transferRepository.findByIdForUpdate(eq(1L))).thenReturn(java.util.Optional.of(pending));
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

            Transfer out = service.reject(1L, "admin-1", "金额可疑");

            assertThat(out.getStatus()).isEqualTo(TransferStatus.REJECTED);
            assertThat(out.getRejectReason()).isEqualTo("金额可疑");
            verify(alipayGateway, never()).transfer(any());
        }
    }
```

- [ ] **Step 4: 跑测试**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.application.service.TransferApplicationServiceTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/repository/TransferRepository.java \
  src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java \
  src/test/java/com/eagle/payment/core/application/service/TransferApplicationServiceTest.java
git commit -m "feat(payment): TransferApplicationService 新增 approve/reject + 悲观锁查询"
```

---

## Task 10: TransferResponse / TransferMapper 输出新字段

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/interfaces/dto/response/TransferResponse.java`
- Modify: `src/main/java/com/eagle/payment/core/application/mapper/TransferMapper.java`

- [ ] **Step 1: 重写 TransferResponse 加字段**

替换文件为：

```java
package com.eagle.payment.core.interfaces.dto.response;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现单详情响应。
 *
 * @author sunshixiong
 */
@Schema(description = "提现单详情")
public record TransferResponse(
        @Schema(description = "提现单 ID") Long id,
        @Schema(description = "业务提现号") String bizTransferNo,
        @Schema(description = "受理模式") TransferMode mode,
        @Schema(description = "渠道") PaymentChannel channel,
        @Schema(description = "收款方账号") String recipientAccount,
        @Schema(description = "收款方姓名") String recipientName,
        @Schema(description = "提现金额(元)") BigDecimal amount,
        @Schema(description = "提现说明") String reason,
        @Schema(description = "渠道转账单号") String channelTransferNo,
        @Schema(description = "状态") TransferStatus status,
        @Schema(description = "到账时间") LocalDateTime succeededAt,
        @Schema(description = "失败 / 退票原因") String failReason,
        @Schema(description = "审核人 ID") String approverId,
        @Schema(description = "审核通过时间") LocalDateTime approvedAt,
        @Schema(description = "审核拒绝时间") LocalDateTime rejectedAt,
        @Schema(description = "审核拒绝原因") String rejectReason,
        @Schema(description = "创建时间") LocalDateTime createTime
) {
}
```

- [ ] **Step 2: 更新 TransferMapper.toResponse**

替换 `toResponse` 方法体：

```java
    public TransferResponse toResponse(Transfer transfer) {
        if (transfer == null) {
            return null;
        }
        return new TransferResponse(
                transfer.getId(),
                transfer.getBizTransferNo(),
                transfer.getMode(),
                transfer.getChannel(),
                transfer.getRecipientAccount(),
                transfer.getRecipientName(),
                transfer.getAmount(),
                transfer.getReason(),
                transfer.getChannelTransferNo(),
                transfer.getStatus(),
                transfer.getSucceededAt(),
                transfer.getFailReason(),
                transfer.getApproverId(),
                transfer.getApprovedAt(),
                transfer.getRejectedAt(),
                transfer.getRejectReason(),
                transfer.getCreateTime()
        );
    }
```

- [ ] **Step 3: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eagle/payment/core/interfaces/dto/response/TransferResponse.java \
  src/main/java/com/eagle/payment/core/application/mapper/TransferMapper.java
git commit -m "feat(payment): TransferResponse/Mapper 输出 mode + 审核字段"
```

---

## Task 11: Admin DTO（ApproveTransferRequest / RejectTransferRequest / TransferAdminQueryRequest）

**Files:**
- Create: `src/main/java/com/eagle/payment/core/interfaces/dto/request/ApproveTransferRequest.java`
- Create: `src/main/java/com/eagle/payment/core/interfaces/dto/request/RejectTransferRequest.java`
- Create: `src/main/java/com/eagle/payment/core/interfaces/dto/request/TransferAdminQueryRequest.java`

- [ ] **Step 1: ApproveTransferRequest**

```java
package com.eagle.payment.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * Admin 审核通过请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "审核通过请求")
public class ApproveTransferRequest {

    @Size(max = 512)
    @Schema(description = "审核备注 (可选)", example = "金额合规已核对")
    @Nullable
    private String remark;
}
```

- [ ] **Step 2: RejectTransferRequest**

```java
package com.eagle.payment.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin 审核拒绝请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "审核拒绝请求")
public class RejectTransferRequest {

    @NotBlank
    @Size(max = 512)
    @Schema(description = "拒绝原因",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "金额可疑,需补充资料")
    private String reason;
}
```

- [ ] **Step 3: TransferAdminQueryRequest**

```java
package com.eagle.payment.core.interfaces.dto.request;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Admin 提现单列表查询条件。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "提现单列表查询条件")
public class TransferAdminQueryRequest {

    @Schema(description = "受理模式")
    @Nullable
    private TransferMode mode;

    @Schema(description = "状态")
    @Nullable
    private TransferStatus status;

    @Schema(description = "渠道")
    @Nullable
    private PaymentChannel channel;

    @Schema(description = "业务提现号 (模糊不支持,精确匹配)")
    @Nullable
    private String bizTransferNo;

    @Schema(description = "创建时间区间起")
    @Nullable
    private LocalDateTime createTimeFrom;

    @Schema(description = "创建时间区间止")
    @Nullable
    private LocalDateTime createTimeTo;
}
```

- [ ] **Step 4: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/eagle/payment/core/interfaces/dto/request/ApproveTransferRequest.java \
  src/main/java/com/eagle/payment/core/interfaces/dto/request/RejectTransferRequest.java \
  src/main/java/com/eagle/payment/core/interfaces/dto/request/TransferAdminQueryRequest.java
git commit -m "feat(payment): 新增 Admin 端 Transfer DTO (Approve/Reject/Query)"
```

---

## Task 12: Repository 加 Specification 分页查询

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/domain/repository/TransferRepository.java`

- [ ] **Step 1: TransferRepository extends JpaSpecificationExecutor**

修改接口签名为：

```java
public interface TransferRepository extends JpaRepository<Transfer, Long>,
        JpaSpecificationExecutor<Transfer> {
```

imports 增加：`import org.springframework.data.jpa.repository.JpaSpecificationExecutor;`

- [ ] **Step 2: 增加列表查询应用层方法（在 TransferApplicationService）**

打开 `TransferApplicationService.java`，在 `findByBizTransferNo` 之后追加：

```java
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Transfer> queryForAdmin(
            com.eagle.payment.core.interfaces.dto.request.TransferAdminQueryRequest query,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Transfer> spec =
                (root, q, cb) -> {
                    java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
                    if (query.getMode() != null) {
                        ps.add(cb.equal(root.get("mode"), query.getMode()));
                    }
                    if (query.getStatus() != null) {
                        ps.add(cb.equal(root.get("status"), query.getStatus()));
                    }
                    if (query.getChannel() != null) {
                        ps.add(cb.equal(root.get("channel"), query.getChannel()));
                    }
                    if (query.getBizTransferNo() != null) {
                        ps.add(cb.equal(root.get("bizTransferNo"), query.getBizTransferNo()));
                    }
                    if (query.getCreateTimeFrom() != null) {
                        ps.add(cb.greaterThanOrEqualTo(root.get("createTime"), query.getCreateTimeFrom()));
                    }
                    if (query.getCreateTimeTo() != null) {
                        ps.add(cb.lessThan(root.get("createTime"), query.getCreateTimeTo()));
                    }
                    return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
                };
        return transferRepository.findAll(spec, pageable);
    }
```

> 实际编码时把 import 提到 import 区，这里展示完整路径以便上下文不丢。

- [ ] **Step 3: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eagle/payment/core/domain/repository/TransferRepository.java \
  src/main/java/com/eagle/payment/core/application/service/TransferApplicationService.java
git commit -m "feat(payment): TransferRepository 接入 JpaSpecificationExecutor + admin 分页查询"
```

---

## Task 13: TransferAdminController + MockMvc 测试

**Files:**
- Create: `src/main/java/com/eagle/payment/core/interfaces/controller/TransferAdminController.java`
- Create: `src/test/java/com/eagle/payment/core/interfaces/controller/TransferAdminControllerTest.java`

- [ ] **Step 1: TransferAdminController**

```java
package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.TransferMapper;
import com.eagle.payment.core.application.service.TransferApplicationService;
import com.eagle.payment.core.interfaces.dto.request.ApproveTransferRequest;
import com.eagle.payment.core.interfaces.dto.request.RejectTransferRequest;
import com.eagle.payment.core.interfaces.dto.request.TransferAdminQueryRequest;
import com.eagle.payment.core.interfaces.dto.response.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer 管理后台 REST 入口:审核 / 列表 / 详情。
 *
 * <p>权限:{@code payment:transfer:approve}。审核者身份从 JWT subject 取,前端不传。
 *
 * @author sunshixiong
 */
@Tag(name = "提现 (管理后台)", description = "审核 / 拒绝 / 待审列表")
@RestController
@RequestMapping("/admin/transfers")
@RequiredArgsConstructor
public class TransferAdminController {

    private final TransferApplicationService transferApplicationService;
    private final TransferMapper mapper;

    @Operation(summary = "审核提现列表",
            description = "支持按 mode / status / channel / bizTransferNo / 时间区间过滤")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @GetMapping
    public Page<TransferResponse> list(@ParameterObject TransferAdminQueryRequest query,
                                       @ParameterObject
                                       @Parameter(description = "分页参数")
                                       @PageableDefault Pageable pageable) {
        return transferApplicationService.queryForAdmin(query, pageable)
                .map(mapper::toResponse);
    }

    @Operation(summary = "查询提现单详情")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) {
        return mapper.toResponse(transferApplicationService.findById(id));
    }

    @Operation(summary = "审核通过 (同事务调渠道)")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @PostMapping("/{id}/approve")
    public TransferResponse approve(@PathVariable Long id,
                                    @Valid @RequestBody ApproveTransferRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(transferApplicationService.approve(
                id, jwt.getSubject(), request.getRemark()));
    }

    @Operation(summary = "审核拒绝 (必填理由)")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @PostMapping("/{id}/reject")
    public TransferResponse reject(@PathVariable Long id,
                                   @Valid @RequestBody RejectTransferRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(transferApplicationService.reject(
                id, jwt.getSubject(), request.getReason()));
    }
}
```

- [ ] **Step 2: TransferAdminControllerTest (MockMvc 切片)**

```java
package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.TransferMapper;
import com.eagle.payment.core.application.service.TransferApplicationService;
import com.eagle.payment.core.domain.model.aggregate.Transfer;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferAdminController.class)
@DisplayName("TransferAdminController")
class TransferAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // Spring Boot 4 推荐 @MockitoBean;若仍用 @MockBean (deprecated) 也能跑
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private TransferApplicationService transferApplicationService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private TransferMapper transferMapper;

    private Transfer sample() {
        return Transfer.create("TRN-001", TransferMode.APPROVAL, PaymentChannel.ALIPAY,
                "user@example.com", "张三", new BigDecimal("500.00"), "结算");
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("无 payment:transfer:approve 权限应 403")
        @WithMockUser(authorities = "other:role")
        void shouldReject403WithoutAuthority() throws Exception {
            mockMvc.perform(post("/admin/transfers/{id}/approve", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("remark", "ok"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("有权限 + JWT subject 应转发到 service.approve(id, subject, remark)")
        void shouldCallServiceApprove() throws Exception {
            when(transferApplicationService.approve(eq(1L), eq("admin-1"), eq("ok")))
                    .thenReturn(sample());
            when(transferMapper.toResponse(any(Transfer.class)))
                    .thenReturn(null);

            mockMvc.perform(post("/admin/transfers/{id}/approve", 1L)
                            .with(csrf())
                            .with(jwt()
                                    .jwt(j -> j.subject("admin-1"))
                                    .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("payment:transfer:approve")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("remark", "ok"))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("reject 缺 reason 应 400")
        void shouldReject400WhenReasonBlank() throws Exception {
            mockMvc.perform(post("/admin/transfers/{id}/reject", 1L)
                            .with(csrf())
                            .with(jwt()
                                    .jwt(j -> j.subject("admin-1"))
                                    .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("payment:transfer:approve")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", ""))))
                    .andExpect(status().isBadRequest());
        }
    }
}
```

> 如项目 `@WebMvcTest` 默认带的 Spring Security 配置过严无法直接通过，参考其他 controller 测试（如果有）的 `@AutoConfigureMockMvc(addFilters = false)` 或自定义测试 SecurityConfig。先按现行配置跑，遇报错再调。

- [ ] **Step 3: 跑测试**

Run: `./gradlew :eagle-services:eagle-payment-service:test --tests "com.eagle.payment.core.interfaces.controller.TransferAdminControllerTest"`
Expected: BUILD SUCCESSFUL（如 Security 配置导致 401 而非 403/200，按行内提示加 `@AutoConfigureMockMvc(addFilters = false)` 或显式注入测试用 SecurityFilterChain；本任务 step 中遇到则补一个 step）

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eagle/payment/core/interfaces/controller/TransferAdminController.java \
  src/test/java/com/eagle/payment/core/interfaces/controller/TransferAdminControllerTest.java
git commit -m "feat(payment): 新增 TransferAdminController + MockMvc 切片测试"
```

---

## Task 14: TransferApprovedIntegrationEvent / TransferRejectedIntegrationEvent

**Files:**
- Create: `src/main/java/com/eagle/payment/core/infrastructure/messaging/TransferApprovedIntegrationEvent.java`
- Create: `src/main/java/com/eagle/payment/core/infrastructure/messaging/TransferRejectedIntegrationEvent.java`

- [ ] **Step 1: TransferApprovedIntegrationEvent**

```java
package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核通过跨服务集成事件。topic {@code payment_transfer_events}, tag {@code approved}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class TransferApprovedIntegrationEvent extends BaseEvent {

    private Long transferId;
    private String bizTransferNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String recipientAccount;
    private String approverId;
    private LocalDateTime approvedAt;

    public TransferApprovedIntegrationEvent(Long transferId, String bizTransferNo,
                                            PaymentChannel channel, BigDecimal amount,
                                            String recipientAccount, String approverId,
                                            LocalDateTime approvedAt) {
        this.transferId = transferId;
        this.bizTransferNo = bizTransferNo;
        this.channel = channel;
        this.amount = amount;
        this.recipientAccount = recipientAccount;
        this.approverId = approverId;
        this.approvedAt = approvedAt;
    }
}
```

- [ ] **Step 2: TransferRejectedIntegrationEvent**

```java
package com.eagle.payment.core.infrastructure.messaging;

import com.eagle.common.event.BaseEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现审核拒绝跨服务集成事件。topic {@code payment_transfer_events}, tag {@code rejected}。
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
public class TransferRejectedIntegrationEvent extends BaseEvent {

    private Long transferId;
    private String bizTransferNo;
    private PaymentChannel channel;
    private BigDecimal amount;
    private String recipientAccount;
    private String approverId;
    private String rejectReason;
    private LocalDateTime rejectedAt;

    public TransferRejectedIntegrationEvent(Long transferId, String bizTransferNo,
                                            PaymentChannel channel, BigDecimal amount,
                                            String recipientAccount, String approverId,
                                            String rejectReason, LocalDateTime rejectedAt) {
        this.transferId = transferId;
        this.bizTransferNo = bizTransferNo;
        this.channel = channel;
        this.amount = amount;
        this.recipientAccount = recipientAccount;
        this.approverId = approverId;
        this.rejectReason = rejectReason;
        this.rejectedAt = rejectedAt;
    }
}
```

- [ ] **Step 3: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/eagle/payment/core/infrastructure/messaging/TransferApprovedIntegrationEvent.java \
  src/main/java/com/eagle/payment/core/infrastructure/messaging/TransferRejectedIntegrationEvent.java
git commit -m "feat(payment): 新增 TransferApproved/Rejected 集成事件 (tag approved/rejected)"
```

---

## Task 15: TransferIntegrationEventPublisher 加 onApproved / onRejected

**Files:**
- Modify: `src/main/java/com/eagle/payment/core/infrastructure/event/TransferIntegrationEventPublisher.java`

- [ ] **Step 1: 加 2 个事件处理器**

在 `onTransferReturned` 方法之后追加：

```java
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferApproved(com.eagle.payment.core.domain.event.TransferApprovedEvent event) {
        publisher.publish(TOPIC, "approved",
                new com.eagle.payment.core.infrastructure.messaging.TransferApprovedIntegrationEvent(
                        event.transferId(), event.bizTransferNo(), event.channel(),
                        event.amount(), event.recipientAccount(),
                        event.approverId(), event.approvedAt()));
        log.debug("published transfer.approved, transferId={}", event.transferId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferRejected(com.eagle.payment.core.domain.event.TransferRejectedEvent event) {
        publisher.publish(TOPIC, "rejected",
                new com.eagle.payment.core.infrastructure.messaging.TransferRejectedIntegrationEvent(
                        event.transferId(), event.bizTransferNo(), event.channel(),
                        event.amount(), event.recipientAccount(),
                        event.approverId(), event.rejectReason(), event.rejectedAt()));
        log.debug("published transfer.rejected, transferId={}", event.transferId());
    }
```

> 编写时把 import 提到 import 区，省去完整类名。Javadoc 顶部类注释里 tag 描述补全为
> `success / failed / returned / approved / rejected`。

- [ ] **Step 2: 编译确认**

Run: `./gradlew :eagle-services:eagle-payment-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/eagle/payment/core/infrastructure/event/TransferIntegrationEventPublisher.java
git commit -m "feat(payment): TransferIntegrationEventPublisher 加 onApproved/onRejected"
```

---

## Task 16: 全量验证 + Modulith 架构测试

**Files:**
- 无新代码改动；仅执行验证命令。

- [ ] **Step 1: 跑全量构建**

Run:
```bash
./gradlew :eagle-services:eagle-payment-service:clean :eagle-services:eagle-payment-service:build
```
Expected: BUILD SUCCESSFUL。

- [ ] **Step 2: 跑 Modulith 架构测试（如果 payment-service 有 ModulithArchitectureTest）**

Run:
```bash
./gradlew :eagle-services:eagle-payment-service:test --tests "*.ModulithArchitectureTest" || echo "no modulith test in payment, skip"
```
Expected: BUILD SUCCESSFUL 或被跳过。

- [ ] **Step 3: 提交剩余未提交内容（如有）**

Run: `git status`
如果有剩余变更（如 Javadoc 调整、import 整理），按内容拆分 commit；否则跳过。

- [ ] **Step 4: 总结性 commit（如本任务没有代码变更，本步可省）**

仅当出现遗漏修复时：

```bash
git add -A
git commit -m "chore(payment): Transfer 审核模式验证修复"
```

---

## Spec 覆盖自检

| 设计稿章节 | 对应任务 |
|---|---|
| §1 状态机 | T2, T3, T6 |
| §2 数据模型 (字段 / 枚举 / 索引 / ddl-auto) | T1, T3 |
| §3 API 端点 (CreateTransferRequest mode / /admin/transfers/**) | T7, T11, T13 |
| §4 ApplicationService 编排 (create 分支 / approve / reject / 悲观锁) | T8, T9, T12 |
| §5 事件 + 错误码 (Approved/Rejected 领域 + 集成事件 / 70050-70053) | T4, T5, T14, T15 |
| §6 测试策略 (单元 + MockMvc) | T3, T6, T8, T9, T13 |

新增 4 个错误码全部用于运行时校验（T6/T8/T9 抛出 + i18n）。审核审计日志（`@AuditLog`）设计稿提到但**未列入本计划**（payment-service 当前未引入 `eagle-audit-log-starter`；如需启用，新增独立任务"接入审计日志"，本次按 YAGNI 留作后续）。

---

## 执行选择

Plan complete and saved to `docs/superpowers/plans/2026-06-08-transfer-approval-mode.md`. 两种执行方式：

**1. Subagent-Driven（推荐 ≤ 15 task 情形，本计划 16 task 接近边界）** —— 我每个 task 派一个全新 subagent，task 间审阅，迭代快但 token 多。

**2. Inline Execution** —— 在本会话内按 `superpowers:executing-plans` skill 批量执行 task，遇 checkpoint 暂停审阅，节省 token。

> 参考用户偏好 memory `feedback_subagent_overhead.md`："长 plan（>15 task）不要逐 task 派 subagent + 双阶段评审，直接执行更快"。本计划 16 task 已超阈值，**默认建议 Inline Execution**。

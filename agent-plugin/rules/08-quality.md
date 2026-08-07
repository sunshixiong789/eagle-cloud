# 代码质量：内聚、耦合与可维护性

前 8 个文件管**边界**（分层、模块、API 契约），本文件管**边界内部**：一个类多大、一个方法多长、
什么时候该抽象、什么时候该容忍重复。

沿用 `00-core.md` 的元规则：只写**有判据、可自查**的项。"命名要清晰""函数要短"这类默认能力已覆盖的
通用建议不写在这里；下表每一行都能在 review 时给出是/否的判定。

---

## 一、规模红线

实测基线（主代码，2026-08）：`ApplicationService` 最大 16 个 public 方法 / 389 行；聚合根最大 556 行（`Account`）。

| 单位 | 软上限 | 硬上限 | 超硬上限时怎么做 |
|---|---|---|---|
| ApplicationService 的 public 方法 | 8 | 12 | 按用例族拆分，读写分离：`XxxApplicationService` + `XxxQueryApplicationService` |
| 单个方法行数（不含注释/空行） | 30 | 50 | 抽同类 private 方法；若抽出来的是业务规则则下沉到领域层 |
| 方法参数个数 | 3 | 5 | 收敛成 `application/command/` 下的 command `record` |
| 条件嵌套层级 | 2 | 3 | 卫语句提前返回；`switch` 模式匹配取代 if 链 |
| 聚合根行数 | 300 | 500 | 检查是否有该拆出去的子聚合或值对象 |

**超限只约束新代码**：现有 `DictApplicationService`（16 方法 / 346 行）、`AccountApplicationService`
（15 方法 / 389 行）已超硬上限，含义是"这次别再往里加方法"，**不是**发起重构 PR 的信号。

---

## 二、贫血模型红线

DDD 项目最容易腐化的地方，也是 ArchUnit 冻结基线里已经出现 `Role.setDataScope` 的原因。

**判据：应用服务里出现"对聚合根状态的条件判断或计算"，该逻辑就属于领域层。**

```java
// ❌ 业务规则泄漏到应用服务：状态机判断 + 状态赋值都在外面
public void cancelOrder(Long id, String reason) {
    Order order = repository.findById(id).orElseThrow(...);
    if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED) {
        throw OrderErrorCode.ORDER_CANNOT_CANCEL.toDomainException();
    }
    order.setStatus(OrderStatus.CANCELLED);
    order.setCancelReason(reason);
    repository.save(order);
}

// ✅ 应用服务只做编排：取聚合 → 调业务方法 → 存
public void cancelOrder(Long id, String reason) {
    Order order = repository.findById(id)
            .orElseThrow(OrderErrorCode.ORDER_NOT_FOUND::toNotFoundException);
    order.cancel(reason);          // 状态机校验 + 状态变更 + registerEvent 都在聚合根内
    repository.save(order);
}
```

应用服务里**允许**出现的判断只有三类：入参预校验、权限校验、跨聚合编排的分支。
`order.getStatus() == X` 这种**读聚合内部状态做业务决策**的写法一律下沉。

---

## 三、DTO 不可变

`interfaces/dto/` 已 100% record 化（原 45 个 `@Data` class 全量迁移完毕）。
写 `@Data` class 会被 `check-java-conventions.sh` hook 直接拦下。

可变 DTO 的真正代价是**分步装配**，迁移前 `UserApplicationService` 里就有 8 处：

```java
// ❌ 迁移前：先拿到对象再逐字段 set，读代码时无法知道对象何时才算「完整」
UserResponse response = mapper.toResponse(user);
response.setRoles(getAssignedRoles(user));
response.setLastLoginAt(logRepository.findLastLogin(user.getId()));
response.setOnline(online);
response.setLoginStatus(online ? "ONLINE" : "OFFLINE");
return response;

// ✅ 迁移后：跨来源数据先备齐，再一次性构造；派生字段（loginStatus）在 record 内部算
return mapper.toResponse(user)
        .withEnrichment(getAssignedRoles(user), lastLogin, online, blacklisted, blacklistId);
```

- DTO 一律 `record`，不加 Lombok
- 需要多来源拼装 → `application/mapper/` 的 `@Component` Mapper 里一次性构造（见 `02-architecture.md`）
- 确实要分两步（先查主体、再挂载关联）→ 在 record 上提供 `withXxx()` 返回新实例，
  **不要**为此退回可变 class
- 树形结构（如 `DictItemResponse.children`）**自底向上构建**：先递归生成子节点，再构造父节点
- record 没有字段初始化器：原 `@Data` 类上的默认值（分页 `page=1`、`accessTokenTtlSeconds=3600`）
  必须搬进**紧凑构造器**兜底，否则 JSON 缺字段时会从默认值变成 `null`

---

## 四、先找现成的，再考虑自己写

模型的默认倾向是**遇到问题就动手实现**。本项目反向约束：**基础设施类的机制，几乎一定已经有人写好了。**

### 4.1 判据

**如果这段代码在实现「重试 / 退避 / 死信搬运 / 线程池 / 连接管理 / 序列化 / 定时调度 / 限流 / 缓存失效 /
分页 / 参数校验」中的任何一种通用机制，停下来先找现成的。** 这些都不是业务逻辑，
自己写的每一行都是要自己维护、自己踩坑、自己补测试的负债。

代码量判据：**能三行解决的不写十行。** 十行手写实现 vs 三行框架配置，后者永远优先 ——
即使前者看起来"更可控"。可控是错觉：框架那三行背后有社区多年的边界条件修复。

### 4.2 决策顺序（自上而下，命中即停）

| 顺位 | 找什么 | 例 |
|---|---|---|
| 1 | 已引入依赖的**扩展点** | `ConnectionFactoryCustomizer`、`RabbitTemplateCustomizer`、`BeanPostProcessor`、各种 `*Configurer` |
| 2 | **中间件 / broker 自身**的能力 | 队列 `x-dead-letter-exchange` 自动转投、`x-message-ttl`、DB 唯一约束、Redis SETNX |
| 3 | 框架的**官方可选依赖** | spring-retry（spring-rabbit 的 optional 依赖，按 `00-core.md` 先进 BOM） |
| 4 | **运维工具，零代码** | RabbitMQ 管理台 "Move messages"、Shovel 插件 |
| 5 | 自己写 | 前四项都确认没有；在 PR 描述里写明确认过程 |

### 4.3 最贵的代价不是多写代码，是架空框架的配置体系

真实案例（`eagle-amqp-starter`，2026-08）：`AmqpListenerRegistrar` 手动
`new DirectMessageListenerContainer(...)` 而不走 Boot 的
`DirectRabbitListenerContainerFactoryConfigurer`，后果是
**`spring.rabbitmq.listener.direct.*` 整套配置对本项目静默失效** ——
`prefetch` / `acknowledge-mode` / `default-requeue-rejected` / `retry.*` 配了都不生效。

这比"多写几十行"严重得多：使用方照官方文档配置，行为却纹丝不动，且没有任何报错或警告。
**手写实现一旦顶替了框架的装配路径，就同时废掉了框架的整个配置面。**

同一个 starter 还手写了两处本可直接复用的机制：

| 手写的 | 现成的 |
|---|---|
| `handleWithRetry()` 退避重试循环 | spring-retry 的 `RetryInterceptorBuilder`，`container.setAdviceChain()` 挂上即可 |
| `sendToDeadLetter()` 死信投递 | `RepublishMessageRecoverer`，还会自动附上 `x-exception-message` / `x-exception-stacktrace` / `x-original-exchange` 等诊断 header |

反例之外的正面对照 —— 同一批可靠性问题，用现成能力每项一行：

| 需求 | 现成方案 |
|---|---|
| nack 的消息不要无限 requeue | `container.setDefaultRequeueRejected(false)`，之后由 **broker** 按队列上的 DLX 自动转投，搬运逻辑一行不写 |
| 停机时正在退避的消息别被推进 DLQ | 抛框架的 `ImmediateRequeueAmqpException`（强制 requeue，不受上一条影响） |
| DLQ 不要无限增长 | `QueueBuilder.ttl()` / `.maxLength()`，由 broker 执行，不需要清理任务 |
| 消费线程被阻塞操作占满 | `ConnectionFactoryCustomizer` + `setSharedExecutor(虚拟线程)` |
| 把死信重投回主队列 | **不写代码** —— RabbitMQ 管理台 / Shovel 插件 |

### 4.4 但「框架提供了接口」不等于「框架替你做了」

反向的坑同样要认：`spring.rabbitmq.publisher-confirm-type=correlated` 只是打开 confirm 通道，
**不注册 `ConfirmCallback` 等于没开**，消息被 broker 拒收照样静默丢失；
`mandatory` 与 `ReturnsCallback`、`@Async` 与 `AsyncUncaughtExceptionHandler` 都是同一类。

**判断方法：查清该扩展点的默认行为。默认是「静默忽略」的，就必须自己接落点** ——
这类实现不算造轮子，是框架明确要求使用方提供的那一半。

### 4.5 抽象的最小化

见到跨类调用就抽接口、见到两处相似就抽基类，同样是"写了不必要的代码"。本项目反向约束：

| 场景 | 抽不抽接口 |
|---|---|
| 单实现、同模块内使用 | **不抽**，直接用 class |
| 需跨 Modulith 模块 / 跨服务协作 | **抽** `Port`（这是 `02-architecture.md` 规定的唯一合法路径） |
| 领域服务需要在 infrastructure 实现（依赖倒置） | **抽**（接口在 `domain/service/`，实现在 `infrastructure/`） |
| 只为"以后可能有第二种实现" | **不抽**，等第二个实现真的出现再抽 |
| 只为单测好 mock | **先别抽** —— 优先构造器注入真实对象；确实需要隔离外部 IO 时才抽 |

同理约束继承：**只有共享状态 + 共享模板方法时才用抽象基类**（如 `AbstractAmqpListener`）。
只共享几个工具方法 → 用无状态静态方法，不要为复用而继承。

---

## 五、复用的归属决策

本仓库有 27 个 starter，最大的耦合风险是**业务概念渗进公共 starter**，一旦渗入，所有服务都被绑死。

| 复用范围 | 归属 |
|---|---|
| 单个类内部 | private 方法 |
| 单模块内多个类 | 同包 package-private helper |
| 单服务跨模块 | 该服务下的共享包；**不要**为此新建 starter |
| 多个服务 | starter；且必须**无业务语义** |

**判据（唯一硬标准）：如果这段代码的入参、返回值或类名里出现业务领域名词
（`Order` / `Account` / `Dict` / `Announcement`），它就不该进 starter。**

重复的容忍度按 **rule of three**：出现第 2 次先复制，第 3 次再抽。过早抽出的"公共方法"
往往因为第 3 个调用方需求不同而长出布尔参数和分支，比重复更难维护。

**禁止**：布尔参数控制分支（`doSave(order, true)`）—— 拆成两个语义明确的方法。

---

## 六、各层厚度

**实况**：现有 3 个 Controller 直接注入了 `Repository`（分层腐化，已在 ArchUnit 基线内）。

| 层 | 只允许做 | 明确禁止 |
|---|---|---|
| Controller | 参数绑定、调用**一个**应用服务方法、返回 | 注入 `Repository`；if/else 业务分支；循环；try-catch（见 `03-api-error.md`） |
| ApplicationService | 事务边界、跨聚合编排、DTO ↔ 领域映射调用 | 聚合内部状态判断（见第二节）；直接拼 SQL |
| Domain | 业务规则、不变式、状态机、领域事件注册 | 依赖 Spring 上下文、依赖 `interfaces` 层类型（含 ErrorCode，应下沉到 domain） |
| Infrastructure | 适配外部（DB / MQ / HTTP / 缓存） | 承载业务规则 |

Controller 方法体超过 5 行，基本就意味着有逻辑该下沉。

---

## 七、starter 的对外 API 演进

starter 被多个服务依赖，破坏性变更的成本远高于业务代码：

- **新增**方法 / 配置项：自由
- **修改**签名、**删除**公开方法、**改**配置键名：破坏性 —— 必须先 `@Deprecated` 保留一个版本，
  Javadoc 写明替代方案，并在 CHANGELOG 记录
- 配置项**默认值**变更等同破坏性变更（使用方不改代码但行为变了），同样要记 CHANGELOG
- starter 公开类的包路径变更 = 删除 + 新增，同上

对应地，**starter 引入即生效，不设 `eagle.xxx.enabled` 总开关**（见 `06-boot4.md`）。

---

## 八、删除纪律

模型默认只加不减，需要显式约束：

- 改造后不再被调用的 private 方法、字段、import → **同一 PR 删掉**，不要留着"可能还有用"
- 被替换的旧实现不要用 `@Deprecated` 留在业务模块里（业务模块无外部使用方，直接删）；
  `@Deprecated` 只用于 starter 对外 API（第七节）
- 注释掉的代码一律删除 —— 历史在 Git 里（`00-core.md` 禁止清单已列）
- **starter 移除时必须同步清理三处**：规则文本、`skills/{name}/`、`verify-rules.sh` 的类清单与 1c 反向校验。
  最近一次移除 9 个 starter（tenant / rocketmq / dynamic-datasource / elasticsearch / excel /
  notification / seata / sentinel / ai）就是先例 —— 漏掉 skill 会让 AI 继续按已删除的 API 写代码，
  比留着死代码危险得多

---

## 九、AI 产出特有的坏味道（提交前自查）

以下是模型倾向做、但在本项目里明确不要的：

- [ ] **手写框架已经提供的机制** —— 重试 / 退避 / 死信搬运 / 线程池 / 序列化 / 调度，
      动手前按第四节的顺序找扩展点；顺带确认没有绕开框架的装配路径（那会静默废掉整片配置）
- [ ] **编造不存在的 API** —— Eagle 专有类拿不准先 `grep` 一个已有用法，不要凭记忆写（`06-boot4.md` 末节）
- [ ] **防御性 null 检查泛滥** —— 已有 23 个 package 标了 `@NullMarked`，参数默认非空，不要层层 `if (x != null)`
- [ ] **try-catch 让代码"能跑通"** —— 吞异常是 `00-core.md` 禁止清单第一条；不确定异常该不该catch 就上抛
- [ ] **预留式设计** —— 未被调用的参数、只有一个实现的接口、没人读的配置项，一律不写
- [ ] **顺手重构无关代码** —— 与本次需求无关的改动不进这个 PR（`00-core.md` Git 一节）
- [ ] **复述代码的注释** —— `// 保存用户` 配 `repository.save(user)` 属于噪音；注释只写**为什么**
- [ ] **一次生成超大类** —— 新建类超过第一节硬上限，说明职责没拆开，先拆再写

---

## 十、守护机制现状

| 守护 | 覆盖范围 |
|---|---|
| `ModulithArchitectureTest` | eagle-system-service、eagle-auth-service |
| `LayeredArchitectureTest` | eagle-system-service（冻结 22 条）、eagle-auth-service（冻结 29 条） |
| `check-java-conventions.sh` hook | 写 Java 前拦截：`@Value`、Jackson 2 包、物理外键、`t_` 表名、`BeanUtils`、DTO 用 `@Data`、`eagle.xxx.enabled` 总开关 |

新增服务时，`ModulithArchitectureTest` + `LayeredArchitectureTest` 两个测试要一起建
（照抄现有两份中的任意一份，只改 `importPackages` 和类注释），并配 `src/test/resources/archunit.properties`
指定冻结 store，否则本文件的分层与厚度规则只能靠人工 review 兜底。

两份 `LayeredArchitectureTest` 的规则集必须保持一致 —— 改一处就改另一处。

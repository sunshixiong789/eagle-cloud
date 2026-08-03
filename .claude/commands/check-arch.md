---
description: 跑 Spring Modulith 架构验证 + 关键 starter 单测，PR 前一键检查
argument-hint: "[模块路径，可选；默认 eagle-base-server:eagle-system-server]"
---

# /check-arch — 架构与构建一键验证

执行 Spring Modulith 架构验证 + 模块单测 + 全量构建，确保 PR 前所有静态约束通过。

## 执行步骤

1. **解析目标模块**
    - 若用户传入 `$ARGUMENTS`，使用该模块路径
    - 否则默认 `eagle-base-server:eagle-system-server`

2. **跑 Modulith 架构验证**

   ```bash
   ./gradlew :{module}:test --tests "*.ModulithArchitectureTest"
   ```

   失败时：
    - 解析输出找出违规依赖（"Module X depends on non-exposed type Y"）
    - 按 `01-architecture.md` 规范判断是否需要：
      a) 给被依赖包加 `@NamedInterface`
      b) 在依赖方 `allowedDependencies` 中声明
      c) 重构通过 Port/Adapter 解耦

3. **跑模块单元测试**

   ```bash
   ./gradlew :{module}:test
   ```

4. **全量构建**

   ```bash
   ./gradlew clean build -x test
   ```

   （仅校验编译 + 静态检查；测试已在第 3 步跑过）

5. **总结输出**
    - ✅ 通过项
    - ❌ 失败项（含原因 + 修复建议）
    - 📊 测试覆盖率（如果配置了 JaCoCo）

## 输出格式

```
=== /check-arch 报告 ===
模块：eagle-base-server:eagle-system-server

[1/3] Modulith 架构验证 ............ ✅ 通过 (2.3s)
[2/3] 单元测试 ..................... ✅ 通过 (47 tests, 1m12s)
[3/3] 全量构建 ..................... ✅ 通过 (38s)

总耗时：2m17s
PR 可提交 ✅
```

或失败示例：

```
[1/3] Modulith 架构验证 ............ ❌ 失败

违规：
  Module 'base' depends on non-exposed type
  com.eagle.system.auth.infrastructure.security.JwtTokenService

建议：
  1) 在 auth/domain/port/ 定义 TokenPort 接口
  2) 在 auth/infrastructure/adapter/ 提供 JwtTokenAdapter 实现
  3) base 改为依赖 TokenPort（参考 01-architecture.md 原则一）

PR 阻塞 ❌
```

## 参考规则

- `01-architecture.md` — Modulith 边界违规处理
- `01-architecture.md` — 跨域依赖原则（Port/Adapter）
- `06-checklist.md` — PR 前完整检查清单

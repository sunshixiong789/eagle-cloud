# ADR-0002：响应不用统一包装类

- 状态：Accepted
- 日期：2026-08-03
- 落地规则：`agent-plugin/rules/03-api-error.md` §响应格式

## 背景

国内 Java 项目普遍用 `ApiResult<T>{code, message, data}` 包装所有响应，HTTP 状态码恒为 200。
本仓库确无此类。

## 决策

**直接返回数据，不做包装。** 语义由 HTTP 状态码承载：创建返 `201`，
资源不存在返 `404`（`toNotFoundException()`），冲突返 `409`。
错误响应由全局异常处理器统一成结构化 JSON，`errorCode` 仅 `AppException` 子类时携带。

## 理由

1. **HTTP 语义已经够用** —— 再造一套 code 体系是重复建模，且两套语义容易打架
   （`HTTP 200 + code:500` 这种响应对网关、CDN、监控、重试中间件全是噪音）。
2. **基础设施可用** —— 网关限流、Sentinel 熔断、客户端重试、APM 错误率统计都按 HTTP 状态码工作；
   恒返 200 会让这些能力全部失效，错误率永远显示 0%。
3. **Spring 生态原生** —— `Page<T>`、`ResponseEntity`、`@ResponseStatus`、SpringDoc 都基于真实状态码；
   包装类要额外写 Advice 和 Schema 适配。

## 代价（明知而接受）

- 前端需要按状态码分支，而非统一读 `code`
- 少数不规范的客户端 / 老旧网关对非 200 处理不佳，需按接入方评估

## 备选方案

**包装 + 恒 200** —— 否决，理由见上第 2 点，代价是不可逆地丢掉整条可观测与容错链路。

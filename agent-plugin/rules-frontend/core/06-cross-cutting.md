# Cross-cutting 模式

## 错误处理

- API 层抛标准错误对象，保留 `status`、`errorCode`、后端 message/key、原始响应摘要。
- Query/Hook 层负责 DTO -> ViewModel、错误归类和 retry 策略。
- Page/Screen 决定 toast、空态、跳转、弹窗。
- Component 只接收渲染用 props，不直接解析 HTTP 错误。

## 401 / 登出

- 401 统一在 HTTP 客户端拦截。
- token 获取通过启动期注入：`configureHttp({ getToken, onUnauthorized })` 或等价机制。
- `shared/api/http` 禁止反向 import `features/auth`、providers、router。

## 主题

- Store 保存原始 `system / light / dark`。
- Provider 负责订阅 store 并调用平台主题 API。
- 组件使用 token / className / 平台主题能力，不直接读 theme store。
- 不双写：同一元素同一属性不要同时 className + style。

## 副作用

- mount/unmount 一次性副作用放 Page/Screen。
- 可复用副作用抽 `hooks/use-*.ts`。
- WebSocket、push、全局事件等 app-level listener 放 Provider 或 app bootstrap。

## 禁止清单

- 每个页面各自处理 401。
- API 层 toast、跳转、读写 UI state。
- `shared` 反向依赖 feature。
- 业务组件直接操作平台全局 listener。

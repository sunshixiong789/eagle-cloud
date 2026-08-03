# 状态、数据流与 Cross-cutting

## 状态归属决策

| 状态类型 | 去处 |
|---|---|
| 服务端数据 | **React Query** |
| 当前组件临时状态 | `useState` / `useReducer` |
| 跨页、跨会话、持久化 UI 状态 | **Zustand + persist** |

**禁止**把服务端数据复制进 Zustand；**禁止**把表单状态放进全局 store。

## React Query

- query key 用元组或 feature 内 key factory：`['orders', filters]`，**不要字符串拼接 key**
- mutation 成功后由 Page/Hook 编排 invalidate；**跨 feature invalidation 必须由 Page 编排**或走公开 key factory，不在 hook 里直接 invalidate 别人的 key
- Web 可按交互设置 `staleTime`；RN/Taro 注意前后台恢复和网络状态

## Zustand

- **必须用 selector**：`useStore(s => s.value)`，不要全量订阅
- persist store 必须有 `_hasHydrated` 或等价 hydration 标记，首帧避免默认值闪烁
- store action 只改本地状态，**不在 store 内 fetch** —— 异步数据由 React Query 管

## React Context

不用 Context 管业务状态。Context 只用于 ThemeProvider、QueryProvider、平台适配器等 framework-level 注入。

---

# Cross-cutting

## 错误处理分工

| 层 | 职责 |
|---|---|
| API | 抛标准错误对象，保留 `status`、`errorCode`、后端 message/key、原始响应摘要 |
| Query / Hook | DTO → ViewModel、错误归类、retry 策略 |
| Page / Screen | 决定 toast、空态、跳转、弹窗 |
| Component | 只接收渲染用 props，**不解析 HTTP 错误** |

## 401 / 登出

- 401 **统一在 HTTP 客户端拦截**，不在每个页面各自处理
- token 获取走启动期注入：`configureHttp({ getToken, onUnauthorized })` 或等价机制
- `shared/api/http` **禁止**反向 import `features/auth`、providers、router

## 主题

- Store 保存原始 `system / light / dark`（不存 resolved 后的值）
- Provider 订阅 store 并调用平台主题 API
- 组件用 token / className / 平台主题能力，**不直接读 theme store**
- 同一元素同一属性不要 className + style 双写

## 副作用

- mount/unmount 一次性副作用放 Page/Screen
- 可复用副作用抽 `hooks/use-*.ts`
- WebSocket、push、全局事件等 app-level listener 放 Provider 或 app bootstrap

## 禁止清单

- 每个页面各自处理 401
- API 层 toast、跳转、读写 UI state
- `shared` 反向依赖 feature
- 业务组件直接操作平台全局 listener

---

# 国际化

## 启用条件

面向多语言用户、后台运营、多端共用文案，或后端返回 i18n key 时启用。纯内部一次性页面可暂不启用，但**不要把后端错误消息写死在组件里**。

## 组织

```text
src/shared/i18n/
src/features/<feature>/i18n/
```

key 用命名空间 `feature.section.name`；错误消息与后端 `errorCode` / i18n key 对齐（后端错误码规范见 `agent-plugin/rules/03-api-error.md`）。

## 使用

- Component 只渲染已翻译文案，**不拼接业务句子**
- 金额、日期、数字由前端按 locale 格式化；后端返回原始值
- 请求带 `Accept-Language`；401/错误拦截统一转换错误码或 key
- 占位符用于变量，避免字符串拼接

## 平台选型

| 平台 | 方案 |
|---|---|
| Web | `i18next` / `react-i18next` |
| RN | 同 Web，注意资源懒加载和本地存储 |
| Taro | 轻量字典封装或兼容 i18next 的薄层 |

## 禁止清单

- 前端写死后端错误中文
- API 层直接 toast 翻译后的文案
- DTO 字段为适配 UI locale 改名或改类型

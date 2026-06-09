# Cross-cutting 模式（通用）

| 关注点 | 归属层 | 范式 |
|---|---|---|
| **错误** | Service / API throw → Hook/Query 透传 → Page 捕获 → UI（toast / modal / inline） | Component 不写 `try/catch`，假设数据已就绪 |
| **加载** | `query.isPending` 在 Page 内决定 skeleton / spinner | Component 默认渲染非加载态 |
| **空态** | Page 处理 `data?.length === 0`，渲染空态组件 | 不下沉到 Component |
| **401 / 登出** | `@shared/api/error.ts` 或 `HttpAuthBridge.onUnauthorized` 统一处理；wiring 在 Provider 完成（`logout()` + 跳登录） | Hook / Page 不重写 |
| **Token 刷新** | `@shared/api/http.ts` 检测 401 + `HttpAuthBridge.getRefreshToken()` 自动 refresh 并重试一次 | 业务调用方完全透明 |
| **主题切换** | Provider + 主题 store；UI 库（antd / ConfigProvider）配 token | Component 不直接读 mode |
| **副作用** | mount/unmount 一次性副作用放 Page 的 `useEffect` | 可复用的副作用抽 `hooks/use-*.ts` |
| **App 级 listener**（WebSocket / push） | 挂载在 `AppProvider` 内，与 QueryProvider 同生命周期 | feature listener 通过 Provider 注入 |

## 错误处理：分层契约

```ts
// 1) API 层 throw 归一化错误
// shared/api/http.ts
class ApiError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) {
    super(message);
  }
}

// 2) Query 层透传（不 catch）
useQuery({ queryKey, queryFn: fetchX, retry: 1 });

// 3) Page 层判断 + UI 反馈
function Page() {
  const { data, isPending, error } = useXQuery();
  if (isPending) return <Skeleton />;
  if (error) {
    if (error instanceof ApiError && error.status === 403) {
      return <NoPermission />;
    }
    return <Alert type="error" message={String(error)} />;
  }
  if (!data) return <Empty />;
  return <Content data={data} />;
}

// 4) Component 不写 try/catch
function Content({ data }: { data: ViewModel }) {
  return <div>{data.name}</div>;   // 假设 data 已就绪
}
```

## 401 / 登出：统一拦截

**禁止**在每个 hook / page 里 `if (error.status === 401) router.push('/login')`。统一在 HTTP 客户端拦截：

```ts
// shared/api/http.ts
configureHttp({
  getToken: () => useAuthStore.getState().token,
  onUnauthorized: async () => {
    // 1) 尝试 refresh
    const refresh = useAuthStore.getState().refreshToken;
    if (refresh) {
      try {
        const next = await refreshAccessToken(refresh);
        useAuthStore.getState().setToken(next.accessToken);
        return next.accessToken;   // 让 http.ts 用新 token 重试一次
      } catch { /* fall through */ }
    }
    // 2) 失败 → 清状态 + 跳登录
    useAuthStore.getState().logout();
    router.replace('/login');
  },
});
```

## 主题切换（分工）

| 层 | 职责 |
|---|---|
| **`@shared/stores/theme.store.ts`** | 保存当前 mode（`'system' \| 'light' \| 'dark'`）+ persist |
| **`@providers/ThemeProvider.tsx`** | 订阅 store + 调用平台主题 API（UI 库 ConfigProvider / `nwColorScheme.set()` / Taro page-meta） |
| **业务 Component** | 通过 className `dark:` 配对响应；**不直接**读 `useThemeStore` |

通用红线：

- **`mode` 必须保存原始三态**（`system / light / dark`），**禁止**保存 resolved 后的 `light / dark`——`system` 才能跟随 OS
- **不双写**：组件不要既 className `bg-surface` 又 `style={{ backgroundColor: theme.surface }}`

平台特有实现见 `platforms/<x>/03-styling.md`：

- Web：UI 库 `ConfigProvider` 接 token（antd / Arco）
- RN：`nwColorScheme.set(mode)`（NativeWind）
- Taro：H5 端 `dark:` className；小程序原生端 `page-meta theme="dark"` 属性 + 系统暗色感知 API

## 副作用

```tsx
// ✅ Mount/unmount 一次性副作用放 Page 的 useEffect
function Page() {
  useEffect(() => {
    const sub = subscribe();
    return () => sub.unsubscribe();
  }, []);
  return <Content />;
}

// ✅ 可复用副作用抽 hook
// hooks/use-online-status.ts
export function useOnlineStatus() {
  const [online, setOnline] = useState(true);
  useEffect(() => {
    /* ... */
  }, []);
  return online;
}
```

## App 级 listener（WebSocket / push / 全局事件）

挂载在 `AppProvider` 内（与 QueryProvider 同生命周期），不挂在 `app/_layout` 或 `main.tsx` 的最外层：

```tsx
function AppProvider({ children }: PropsWithChildren) {
  // ✅ feature 提供的 listener hook 在这里激活
  useWsNotifications();
  usePushPermissions();
  return <ThemeProvider>{children}</ThemeProvider>;
}
```

feature listener 通过 Provider 间接被激活 —— feature 之间不互相 `import` 对方的 listener hook。

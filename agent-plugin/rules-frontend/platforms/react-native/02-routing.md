# React Native — 路由

主流方案：**Expo Router**（文件式路由）。少数项目用 **React Navigation**（集中式）。

## Expo Router（推荐）

### 文件式约定

```
app/
├── _layout.tsx              # 根 layout：挂 AppProvider + Stack 声明
├── index.tsx                # / 首页（薄壳）
├── (tabs)/                  # 底部 tab 分组（括号表示 group，不进 URL）
│   ├── _layout.tsx          # Tabs 配置
│   ├── home.tsx             # /home 薄壳
│   ├── catalog.tsx
│   └── profile.tsx
├── product/
│   ├── [id].tsx             # /product/:id 薄壳（动态参数）
│   └── _layout.tsx          # 可选 nested layout
├── (modal)/                 # modal 路由分组
│   ├── _layout.tsx          # 配 presentation: 'modal'
│   ├── login.tsx
│   └── cart.tsx
└── +not-found.tsx           # 404 兜底
```

### 薄壳约定（路由文件 1 行）

```tsx
// app/product/[id].tsx —— 仅 1 行 re-export
export { default } from '@features/product/screens/ProductDetailScreen';
```

```tsx
// app/(tabs)/home.tsx
export { default } from '@features/home/screens/HomeScreen';
```

**路由文件本身不写业务**——业务一律放在 `@features/<f>/screens/`。

### 根 layout

```tsx
// app/_layout.tsx
import { Stack } from 'expo-router';
import { AppProvider } from '@providers/AppProvider';

export default function RootLayout() {
  return (
    <AppProvider>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="(modal)" options={{ presentation: 'modal' }} />
        <Stack.Screen name="+not-found" />
      </Stack>
    </AppProvider>
  );
}
```

### Screen 内读路由参数

```tsx
// src/features/product/screens/ProductDetailScreen.tsx
import { useLocalSearchParams } from 'expo-router';

export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const numericId = Number(id);
  const { data, isPending } = useProductQuery(numericId);
  // ...
}
```

### 导航

```tsx
import { useRouter } from 'expo-router';

const router = useRouter();
router.push(`/product/${productId}`);
router.replace('/login');
router.back();
```

**只在 Screen 内导航**，Component 不调 `useRouter`。

---

## React Navigation（备选，集中式）

```tsx
// src/app/Navigator.tsx
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

const Stack = createNativeStackNavigator();

export function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen name="ProductDetail" component={ProductDetailScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
```

- 集中声明所有 Screen，**lazy 不强求**（RN bundle 一次性下发）
- 导航参数类型化用 `NativeStackScreenProps<RootStackParamList, 'ProductDetail'>`

## 禁止清单

- 禁止把根 `app/` 迁到 `src/app/`（Expo Router 配置代价大）
- 禁止路由文件超过 1 行（必须 `export { default } from '...'`）
- 禁止路由文件 import feature 内部 hook/store/api
- 禁止 Component 内调 `useRouter` / `navigation.navigate`（导航属于 Screen 编排）
- 禁止跨 Screen 共享导航状态（用 Zustand persist 或 URL 参数）
- 禁止 deep link 处理散落各处——`app/_layout.tsx` 顶层 `useEffect` 统一处理

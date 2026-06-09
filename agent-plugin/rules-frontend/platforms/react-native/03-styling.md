# React Native — 样式（NativeWind）

> 通用样式红线（不双写、style 白名单）见 `core/08-red-lines.md` §5。

## 三角色

| 工具 | 用途 | 例子 |
|---|---|---|
| **NativeWind className** | 所有 RN 视图组件的样式（布局 + 主题色） | `<View className="bg-surface dark:bg-surface-dark p-4">` |
| **`theme.X`**（`getEcommerceTheme()` 或同类返回值） | **仅**作为 RN 原生 prop 的颜色字符串 | `<Icon color={theme.X}>` |
| **`nwColorScheme.set(mode)`** | 主题切换 API | `nwColorScheme.set('system')` |

## 红线

### 1. 主题色必须 className 化

```tsx
// ✅ className + dark: 配对，NativeWind 接管深浅色
<View className="bg-surface dark:bg-surface-dark">
  <Text className="text-text dark:text-text-dark">...</Text>
</View>

// ❌ 用 style 写主题色
<View style={{ backgroundColor: '#fff' }} />        // 不响应深色模式
<View style={{ backgroundColor: theme.surface }} /> // 主题色应该用 className
```

### 2. `theme.X` 只能作为 RN 原生 prop 的颜色字符串，不写进 style

适用：

```tsx
<Icon color={theme.primary}>                       // ✅ 原生 prop
<TextInput placeholderTextColor={theme.muted}>     // ✅ 原生 prop
<LinearGradient colors={[theme.start, theme.end]}> // ✅ 原生 prop
```

不适用（用 className 替代）：

```tsx
// ❌ style 主题色
<View style={{ backgroundColor: theme.surface }}>
// ✅ className
<View className="bg-surface dark:bg-surface-dark">
```

### 3. `nwColorScheme.set()` 必须传原始 `mode`

```tsx
// ✅ 传 'system' / 'light' / 'dark' 原始字符串
import { nwColorScheme } from 'nativewind';
nwColorScheme.set('system');
nwColorScheme.set('dark');

// ❌ 禁止：传 resolved 后的具体值
const resolved = mode === 'system' ? (sysMode ?? 'light') : mode;
nwColorScheme.set(resolved);   // 持久锁死 Appearance
```

**理由**：底层走 `Appearance.setColorScheme(value)`。传 `'system'` 时内部调 `setColorScheme('unspecified')` 解锁系统跟随；传具体值会持久锁死，跨重启都丢失系统跟随能力。

### 4. style 白名单

下列场景可保留 `style={{ }}`：

- **RN 阴影 props**（无对应 utility）：
  ```tsx
  <View style={{
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  }} />
  ```
- **reanimated worklet 返回值**：
  ```tsx
  const animStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: y.value }],
  }));
  <Animated.View style={animStyle} />
  ```
- **动态尺寸 / 颜色**（来自 props / state / Dimensions / hook）：
  ```tsx
  <View style={{ width: containerWidth * 0.8, height: containerWidth * 0.8 }} />
  ```
- **CSS border-trick 三角箭头**等少量几何 hack

## NativeWind 类名约定

- 使用 Tailwind class 风格：`bg-` / `text-` / `border-` / `p-` / `m-` / `flex-` / `gap-`
- 主题色用项目自定义 token：`bg-surface dark:bg-surface-dark`、`text-text dark:text-text-dark`、`text-muted`
- 不用 Tailwind 任意值色：`bg-[#3b82f6]` ❌（硬编码无主题感知）

## 主题切换 wiring

```tsx
// providers/ThemeProvider.tsx
import { nwColorScheme } from 'nativewind';
import { useThemeStore } from '@shared/stores/theme.store';

export function ThemeProvider({ children }: PropsWithChildren) {
  const mode = useThemeStore(s => s.mode);
  useEffect(() => {
    nwColorScheme.set(mode);   // ✅ 传原始 mode
  }, [mode]);
  return <NavigationThemeProvider>{children}</NavigationThemeProvider>;
}
```

> 业务 Component 不读 `useThemeStore`——通过 `dark:` className 自动响应（通用规则见 `core/06-cross-cutting.md` 主题切换分工）。

## 禁止清单

- 禁止 `style={{ backgroundColor }}` 写主题色（用 className）
- 禁止 className + `style={{ color }}` 双写
- 禁止 `theme.X` 写进 `style`（仅原生 prop 使用）
- 禁止 `nwColorScheme.set(resolvedMode)`（必须传原始 'system' / 'light' / 'dark'）
- 禁止 Tailwind 任意值色 `bg-[#xxx]`（无主题）
- 禁止业务 Component 直接读 `useThemeStore.mode`

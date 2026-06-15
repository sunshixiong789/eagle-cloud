# 扩展信号（Scaling Signals，通用）

当前 Feature-Sliced 已够用。出现下列信号时再考虑进一步精细化：

| 信号 | 应对 |
|---|---|
| **单个 feature 文件数 > 30** | 在 feature 内引入 `widgets/` 或 `entities/` 子分层（趋近完整 FSD） |
| **多 feature 都需要某领域规则**（如优惠券计算、权限计算） | 抽 `@shared/domain/<rule>.ts` 纯函数库 |
| **出现真正的领域聚合行为**（订单状态机、退款流程、审批流） | 引入显式 Use Case 层 `@features/<f>/use-cases/*.ts`（趋近 Clean Architecture） |
| **HTTP / 存储 等基础设施需要切实现** | 给依赖反转模式更多 Port 接口；启用 / 扩展 `src/infrastructure/`（趋近完整 Hexagonal） |
| **团队规模 > 3 人或 feature 数 > 15** | 评估 monorepo / 子包拆分（pnpm workspace / yarn workspace / nx / turborepo） |
| **同源代码跨多端**（H5 + 小程序 + RN） | 评估 Taro / Uni-App；或将 feature 内部纯业务函数（无 UI）抽 npm 包共享 |
| **状态库瓶颈**（Zustand store 文件 > 10 个、跨 store 联动复杂） | 评估 Redux Toolkit / Jotai / XState（状态机） |
| **页面冷启动慢**（首屏 > 3s） | feature 级 code-split、路由级 lazy、依赖体积审计 |

每次架构升级**前**先核对这张表，避免过度设计。

---

## 反向信号：过度抽象

下面这些是"过度抽象"的味道，出现时**应当回退**：

- Feature 内部 80% 文件都依赖 `@shared/utils/` 的工具 → 是不是把业务逻辑误抽到 shared 了？
- 一个 hook 包了 5 层其他 hook，最底层只调 1 个 API → 拍扁
- DTO / ViewModel / Component Props 三层映射代码量超过实际渲染代码 → 简化为两层（直接传 ViewModel 给 Component）
- 路由表里超过 30 个 lazy import 但没用 chunk 分组 → 按业务域分组打包，而非"每个 page 一个 chunk"
- 引入 use-case 层但只有 1 个 use-case → 撤回

---

## 监控点

每个项目应该有自己的体量监控（在 CI 输出或 PR 模板里）：

- `src/features/<x>/` 文件数 → 单 feature 触顶 30 报警
- bundle size（按 chunk）→ 关键 chunk 触顶报警（Web 主包 > 500KB / 小程序主包 > 1.5MB）
- 单文件行数 → 单文件 > 500 行考虑拆分
- 循环依赖数 → 必须为 0（dependency-cruiser CI 强制，详见 `99-dependency-check.md`）

具体阈值由项目自定。

# 代码规范（Google Java Style）

遵循 Google Java Style Guide 全文。

**格式：**

- 缩进：4 个空格，禁止 Tab
- 行宽限制：100 字符
- 大括号：K&R 风格，即使单行语句也不省略大括号
- 每个 `import` 单独一行，禁止通配符导入（`import java.util.*`）
- 类成员顺序：`static fields → instance fields → constructors → methods`

**注释：**

- 所有 `public` 类、接口、方法必须有 Javadoc（`/** ... */`）
- 实现细节使用行内注释（`//`），说明「为什么」而非「做什么」
- 实体类字段使用字段级 Javadoc
- 禁止提交无意义注释（`// TODO`、`// test`、`// 临时`）不加说明

**Lombok 使用：**

- 聚合根：`@Getter @NoArgsConstructor`（通过业务方法修改状态，不暴露 setter；使用静态工厂方法创建，不用 @Builder）
- 子实体：`@Getter @Setter @NoArgsConstructor`（由聚合根级联管理，需要 setter）
- 值对象（`@Embeddable`）：`@Getter @AllArgsConstructor @NoArgsConstructor`（不可变，无 setter）
- Service / Controller 使用 `@RequiredArgsConstructor`（替代 `@Autowired`）
- 禁止在 JPA 实体类上使用 `@Data`（影响 equals/hashCode 行为，导致延迟加载和集合操作异常）
- 禁止在 JPA 实体上使用 `@Builder`（与 Hibernate 代理、延迟加载不兼容）

**空安全：**

- 新增模块的 `package-info.java` 必须添加 JSpecify `@NullMarked` 注解，声明包级空安全

**其他：**

- 禁止使用原始类型（Raw Types），如 `List` 应写 `List<String>`
- 禁止捕获 `Exception` / `Throwable` 后静默吞掉
- 方法长度建议不超过 50 行，超过时考虑拆分
- 禁止魔法数字，使用命名常量或枚举替代

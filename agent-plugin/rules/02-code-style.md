# 代码风格规范

遵循现有代码风格和 Google Java Style。模型默认可处理的格式细节不在本规则展开；只记录 Eagle 项目需要额外坚持的约束。

## 格式红线

- Java 使用 4 空格缩进、K&R 大括号、单 import，不使用通配符导入。
- 保持行宽约 100 字符；格式化不要夹带无关重排。
- 新代码不提交注释掉的代码、无说明的 TODO、临时代码和调试输出。

## 注释

- 公共 API、配置属性、JPA 实体字段需要有能解释业务含义的 Javadoc。
- 注释说明“为什么”和约束背景，不复述代码做了什么。

## Lombok

- 聚合根：`@Getter @NoArgsConstructor`，通过业务方法改状态，使用静态工厂创建，不暴露 setter，不用 `@Builder`。
- 子实体：`@Getter @Setter @NoArgsConstructor`，由聚合根级联管理。
- 值对象：优先不可变；JPA `@Embeddable` 需要满足 Hibernate 构造要求。
- Service / Controller 用构造器注入，通常配合 `@RequiredArgsConstructor`。
- JPA 实体禁止 `@Data` 和 `@Builder`。

## 空安全与配置

- 新增模块的 `package-info.java` 添加 JSpecify `@NullMarked`。
- 配置注入统一使用 `@ConfigurationProperties(prefix = "eagle.xxx")`，Properties 放在 `infrastructure/config/`。
- 禁止 `@Value` 注入普通配置；`Resource` 类路径注入例外见 `00-collaboration.md`。

## 禁止清单

- 静默吞掉 `Exception` / `Throwable`。
- 用原始类型（Raw Types）。
- 魔法数字散落在业务逻辑中。
- 为“省事”引入反射式 copy、全局格式化或无关重构。

# 单元测试规范（Unit Testing）

**框架：** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)

## 架构测试

使用 Spring Modulith 的项目，PR 前必须运行架构验证测试，确保：

- 模块间无循环依赖
- 无非法跨模块访问（访问其他模块内部包）
- 模块依赖符合 `allowedDependencies` 声明

## 文件组织

- 测试文件位于 `src/test/java/`，包路径与被测类完全一致
- Controller 测试：`{Name}ControllerTest.java`
- Service 测试：`{Name}ServiceTest.java`

## 测试结构（AAA 模式）

```java

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryValidationService inventoryService;
    @InjectMocks
    private OrderApplicationService orderApplicationService;

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("should create order when stock is sufficient")
        void shouldCreateOrderWhenStockIsSufficient() {
            // Arrange
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doNothing().when(inventoryService).validateStock(anyLong(), anyInt());

            // Act
            orderApplicationService.createOrder(request);

            // Assert
            verify(inventoryService).validateStock(PRODUCT_ID, QUANTITY);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw DomainException when stock is insufficient")
        void shouldThrowWhenStockInsufficient() {
            doThrow(OrderErrorCode.INSUFFICIENT_STOCK.toDomainException())
                    .when(inventoryService).validateStock(anyLong(), anyInt());

            assertThrows(DomainException.class,
                    () -> orderApplicationService.createOrder(request));
        }
    }
}
```

## 命名规范

- 测试方法：`should{期望行为}When{前提条件}`
- 使用 `@Nested` + `@DisplayName` 按被测方法分组
- 测试常量统一定义在类顶部（`private static final`）

## 覆盖要求

- 每个 Service 方法至少覆盖：正常路径、边界条件、异常路径
- 领域模型业务方法（聚合根方法）必须有单元测试
- 禁止只写 happy path，必须覆盖异常分支

## Mock 使用原则

- 只 mock 外部依赖（Repository、外部 Service、领域服务）
- 不得 mock 被测类本身
- 使用 `verify()` 验证关键方法调用
- 使用 `ArgumentCaptor` 验证传入参数的具体内容

## 断言

- 使用 JUnit 5 原生断言（`assertEquals`、`assertTrue`、`assertNotNull` 等）
- 每个测试方法只验证一个行为
- 异常测试使用 `assertThrows` 并断言异常类型为 DDD 异常体系中的类型

## 禁止

- 禁止在单元测试中连接真实数据库、网络或文件系统
- 禁止测试方法之间有执行顺序依赖
- 禁止注释掉测试代码提交（用 `@Disabled("reason")` 或直接删除）
- 禁止使用 `Thread.sleep()` 做时序控制

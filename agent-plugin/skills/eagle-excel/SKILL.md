---
name: eagle-excel
description: Use when importing or exporting Excel files in eagle-cloud — ExcelReader.read(InputStream, Class) for import, ExcelWriter.write(List, Class) / writeTo(List, Class, OutputStream) for export, @ExcelColumn annotation for field mapping, auto SXSSF streaming for large datasets
---

# eagle-excel-starter — Excel 导入导出（Apache POI）

## 何时使用

- Controller 接收 `MultipartFile` 批量导入业务数据（订单、用户、商品等）
- Controller 导出业务数据为 `.xlsx` 文件下载
- 数据量 > 10000 行需要流式写入避免 OOM

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-excel-starter')
```

```yaml
eagle:
  excel:
    default-sheet-name: "Sheet1"    # 导出默认 Sheet 名
    date-format: "yyyy-MM-dd"       # LocalDate 全局格式
    datetime-format: "yyyy-MM-dd HH:mm:ss"  # LocalDateTime 全局格式
    streaming-threshold: 10000      # 超过此行数自动切换 SXSSF 流式
    max-rows: 100000                # 导入最大行数限制
```

引入后 `ExcelReader`、`ExcelWriter` 自动注册为 Spring Bean。

## `@ExcelColumn` 注解

```java
public class OrderImportDto {

    @ExcelColumn(value = "订单号", index = 0, required = true)
    private String orderNo;

    @ExcelColumn(value = "下单时间", index = 1, dateFormat = "yyyy/MM/dd HH:mm")
    private LocalDateTime orderTime;

    @ExcelColumn(value = "金额（元）", index = 2)
    private BigDecimal amount;

    @ExcelColumn(value = "状态", index = 3)
    private String status;

    // 无 @ExcelColumn 的字段自动忽略
    private Long internalId;
}
```

| 属性           | 说明                               | 默认     |
|--------------|----------------------------------|--------|
| `value`      | 列标题（导入匹配标题 / 导出表头）               | 必填     |
| `index`      | 列序号（0-based），`-1` 按声明顺序           | `-1`   |
| `dateFormat` | 日期格式，覆盖全局 `date-format`          | 空（用全局）|
| `required`   | 导入时单元格为空是否抛 `IllegalArgumentException` | `false`|

## 核心 API

### 导入（`ExcelReader`）

```java
// read(inputStream, Class) — 读第 0 个 Sheet
List<OrderImportDto> read(InputStream inputStream, Class<T> targetClass) throws IOException;

// read(inputStream, Class, sheetIndex) — 读指定 Sheet
List<OrderImportDto> read(InputStream inputStream, Class<T> targetClass, int sheetIndex) throws IOException;
```

支持字段类型：`String / Integer / Long / Double / BigDecimal / Boolean / LocalDate / LocalDateTime`。

### 导出（`ExcelWriter`）

```java
// write(...) — 返回字节数组
byte[] write(List<T> data, Class<T> clazz) throws IOException;

// writeTo(...) — 直接写入 OutputStream（HTTP 响应直传，推荐）
void writeTo(List<T> data, Class<T> clazz, OutputStream outputStream) throws IOException;
```

超过 `streaming-threshold`（默认 10000）行自动切换 SXSSF 流式（内存窗口 100 行），无需手动配置。

## 最小示例

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ExcelReader excelReader;
    private final ExcelWriter excelWriter;
    private final OrderApplicationService orderService;

    /** 导入 */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResult importOrders(@RequestParam MultipartFile file) throws IOException {
        List<OrderImportDto> rows = excelReader.read(file.getInputStream(), OrderImportDto.class);
        return orderService.batchImport(rows);
    }

    /** 导出 */
    @GetMapping("/export")
    public void exportOrders(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=orders.xlsx");

        List<OrderExportDto> data = orderService.exportAll();
        excelWriter.writeTo(data, OrderExportDto.class, response.getOutputStream());
    }
}

// 导出 DTO
public class OrderExportDto {

    @ExcelColumn(value = "订单号", index = 0)
    private String orderNo;

    @ExcelColumn(value = "下单时间", index = 1)
    private LocalDateTime createTime;

    @ExcelColumn(value = "金额（元）", index = 2)
    private BigDecimal totalAmount;

    @ExcelColumn(value = "状态", index = 3)
    private String status;
}
```

## 大文件导出最佳实践

```java
// 超大数据集：分批查询 + 流式写入（避免 List 撑爆堆内存）
@GetMapping("/export/large")
public void exportLarge(HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=large-orders.xlsx");

    // 分批收集（streaming-threshold 超 10000 自动切 SXSSF）
    List<OrderExportDto> batch = new ArrayList<>();
    Slice<Order> slice;
    int page = 0;
    do {
        slice = orderRepository.findAll(PageRequest.of(page++, 5000));
        batch.addAll(slice.getContent().stream().map(orderMapper::toExportDto).toList());
    } while (slice.hasNext());

    excelWriter.writeTo(batch, OrderExportDto.class, response.getOutputStream());
}
```

## 常见错误

- ❌ DTO 字段无 `@ExcelColumn` 却期望导入 → ✅ 无注解字段自动忽略
- ❌ 导出文件名中文乱码 → ✅ `Content-Disposition` 使用 `URLEncoder.encode(name, UTF_8)`
- ❌ 导入超大 Excel 报 OOM → ✅ 调小 `max-rows` 或要求用户拆分文件；导出超大数据靠 SXSSF 自动解决
- ❌ 日期列出现数字而非日期字符串 → ✅ 确认 `dateFormat` 与实际单元格格式一致

## 关联规则

- `.claude/rules/26-file-storage.md` — 导出文件先写 OSS，再返回签名 URL（大文件）
- `.claude/rules/23-performance.md` — 大数据量流式处理

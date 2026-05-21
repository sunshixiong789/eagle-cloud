package com.eagle.example.sample.interfaces.controller;

import com.eagle.common.dto.Result;
import com.eagle.example.sample.application.command.CreateProductCommand;
import com.eagle.example.sample.application.command.UpdateProductCommand;
import com.eagle.example.sample.application.dto.ProductDto;
import com.eagle.example.sample.application.service.SampleProductApplicationService;
import com.eagle.excel.writer.ExcelWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 商品示例控制器。
 *
 * <p>演示特性：
 * <ul>
 *   <li>SpringDoc OpenAPI 注解（eagle-openapi-starter）</li>
 *   <li>Excel 导出（eagle-excel-starter）</li>
 *   <li>分页查询</li>
 * </ul>
 */
@Tag(name = "示例商品管理", description = "Starter 验证用商品 CRUD 接口")
@RestController
@RequestMapping("/api/sample/products")
@RequiredArgsConstructor
public class SampleProductController {

    private final SampleProductApplicationService productService;
    private final ExcelWriter excelWriter;

    @Operation(summary = "创建商品", description = "演示 @Idempotent + @AuditLog + @RateLimit")
    @PostMapping
    public Result<ProductDto> create(@Valid @RequestBody CreateProductCommand command) {
        return Result.success(productService.create(command));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<ProductDto> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateProductCommand command) {
        return Result.success(productService.update(new UpdateProductCommand(id, command.name(),
                command.price(), command.description(), command.supplierPhone())));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }

    @Operation(summary = "根据 ID 查询商品")
    @GetMapping("/{id}")
    public Result<ProductDto> findById(@PathVariable Long id) {
        return Result.success(productService.findById(id));
    }

    @Operation(summary = "分页查询商品列表")
    @GetMapping
    public Result<Page<ProductDto>> findAll(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return Result.success(productService.findAll(pageable));
    }

    @Operation(summary = "导出商品列表（Excel）")
    @GetMapping("/export")
    @SneakyThrows
    public void export(HttpServletResponse response) {
        Page<ProductDto> page = productService.findAll(
                Pageable.ofSize(1000).withPage(0));
        List<ProductDto> data = page.getContent();

        String fileName = URLEncoder.encode("商品列表.xlsx", StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        excelWriter.writeTo(data, ProductDto.class, response.getOutputStream());
    }
}

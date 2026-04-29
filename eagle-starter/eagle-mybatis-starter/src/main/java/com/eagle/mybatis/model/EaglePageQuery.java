package com.eagle.mybatis.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 统一分页查询入参基类。
 *
 * <p>所有需要分页的 Controller 请求 DTO 均应继承此类，以获得统一的分页、
 * 排序参数绑定和 Bean Validation 校验支持。继承后可追加业务特定的过滤字段。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 请求 DTO 继承此基类
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class QueryUserRequest extends EaglePageQuery {
 *     @Schema(description = "用户名关键词")
 *     private String username;
 * }
 *
 * // 应用服务中转换为 MyBatis-Plus Page 对象
 * Page<User> page = request.toPage();
 * }</pre>
 *
 * @author eagle
 */
@Data
public class EaglePageQuery {

    /**
     * 页码，从 1 开始。
     * 默认第 1 页，最小值为 1。
     */
    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;

    /**
     * 每页返回的记录数。
     * 默认 20 条，最小 1 条，最大 200 条。
     */
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 200, message = "每页大小最大为200")
    private int pageSize = 20;

    /**
     * 排序字段名（驼峰命名，如 {@code createTime}、{@code updateTime}）。
     * 为空时使用默认排序（通常为主键降序）。
     */
    private String orderBy;

    /**
     * 排序方向，支持 {@code "asc"} 或 {@code "desc"}（不区分大小写）。
     * 默认降序。
     */
    private String orderDirection = "desc";

    /**
     * 将当前分页参数转换为 MyBatis-Plus {@link Page} 对象。
     *
     * <p>注意：此方法仅设置页码和每页大小，排序需在调用处通过
     * {@link Page#addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem...)} 追加，
     * 或委托给 {@link com.eagle.mybatis.base.EagleServiceImpl#pageQuery} 统一处理。
     *
     * @param <T> 实体类型（泛型占位，由调用方推断）
     * @return 配置了页码和每页大小的 {@link Page} 对象
     */
    public <T> Page<T> toPage() {
        return new Page<>(pageNum, pageSize);
    }
}

package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户查询请求
 *
 * @author eagle
 * @since 1.0.0
 */
@Schema(description = "用户查询请求")
public record UserQueryRequest(

        @Schema(description = "用户名", example = "zhangsan")
        String username,

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        String email,

        @Schema(description = "姓名", example = "张三")
        String name,

        @Schema(description = "页码", example = "1")
        Integer page,

        @Schema(description = "每页大小", example = "10")
        Integer size
) {

    /** record 无字段初始化器，原 {@code @Data} 类上的分页默认值改在这里兜底。 */
    public UserQueryRequest {
        page = page != null ? page : 1;
        size = size != null ? size : 10;
    }
}

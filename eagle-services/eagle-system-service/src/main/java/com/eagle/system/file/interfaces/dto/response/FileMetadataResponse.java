package com.eagle.system.file.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 文件元数据响应 DTO
 *
 * @author sunshixiong
 */
@Schema(description = "文件元数据")
public record FileMetadataResponse(
        @Schema(description = "文件 ID", example = "10086") Long id,
        @Schema(description = "原始文件名", example = "report.pdf") String originalName,
        @Schema(description = "字节数", example = "204800") Long size,
        @Schema(description = "MIME 类型", example = "application/pdf") String contentType,
        @Schema(description = "上传者用户 ID", example = "1024") String uploadedBy,
        @Schema(description = "下载 / 访问 URL", example = "/api/files/10086") String url,
        @Schema(description = "上传时间") LocalDateTime uploadedAt
) {
}

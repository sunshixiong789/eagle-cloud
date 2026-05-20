package com.eagle.system.file.application.mapper;

import com.eagle.system.file.domain.model.aggregate.FileMetadata;
import com.eagle.system.file.interfaces.dto.response.FileMetadataResponse;
import org.springframework.stereotype.Component;

/**
 * 文件 DTO 映射器（纯 Java 实现，不使用 MapStruct）
 *
 * @author sunshixiong
 */
@Component
public class FileMapper {

    /**
     * 文件下载 URL 模板：调用方自己的 controller endpoint
     */
    private static final String URL_TEMPLATE = "/api/files/%d";

    public FileMetadataResponse toResponse(FileMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        return new FileMetadataResponse(
                metadata.getId(),
                metadata.getOriginalName(),
                metadata.getSize(),
                metadata.getContentType(),
                metadata.getUploadedBy(),
                String.format(URL_TEMPLATE, metadata.getId()),
                metadata.getCreateTime()
        );
    }
}

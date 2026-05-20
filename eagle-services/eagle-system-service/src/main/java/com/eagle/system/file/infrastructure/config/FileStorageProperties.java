package com.eagle.system.file.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

/**
 * 文件上传配置
 *
 * <p>配置示例：
 * <pre>
 * eagle.file:
 *   bucket: eagle-dev-files
 *   max-size-mb: 50
 *   allowed-extensions: jpg,jpeg,png,gif,webp,pdf,docx,xlsx,txt
 * </pre>
 *
 * @author sunshixiong
 */
@Data
@Validated
@ConfigurationProperties(prefix = "eagle.file")
public class FileStorageProperties {

    /**
     * 默认 bucket（local 模式下为子目录名）
     */
    @NotBlank
    private String bucket = "eagle-dev-files";

    /**
     * 单文件大小上限（MB）
     */
    @Min(1)
    private int maxSizeMb = 50;

    /**
     * 允许的文件后缀（不含点）
     */
    @NotEmpty
    private List<String> allowedExtensions = List.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "docx", "xlsx", "txt"
    );

    /**
     * 单租户部署的默认租户 ID（与 t_file.tenant_id 对应）
     */
    @NotBlank
    private String defaultTenantId = "default";

    public long getMaxSizeBytes() {
        return (long) maxSizeMb * 1024 * 1024;
    }

    public Set<String> getAllowedExtensionsLower() {
        return Set.copyOf(allowedExtensions.stream().map(String::toLowerCase).toList());
    }
}

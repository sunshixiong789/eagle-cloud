package com.eagle.system.file.interfaces.controller;

import com.eagle.system.file.application.service.FileApplicationService;
import com.eagle.system.file.application.service.FileApplicationService.DownloadResource;
import com.eagle.system.file.interfaces.dto.response.FileMetadataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理 Controller
 *
 * <p>提供文件上传、下载、元数据查询与软删除接口。
 * 鉴权由 {@code eagle-resource-server-starter} 的 filter chain 强制要求登录，
 * 删除接口在应用服务内做"所有者或 admin"的细粒度校验。
 *
 * @author sunshixiong
 */
@Tag(name = "文件管理", description = "上传 / 下载 / 删除业务文件")
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileApplicationService fileApplicationService;

    @Operation(summary = "上传文件",
            description = "multipart/form-data 上传。后缀白名单、大小上限见 eagle.file 配置。",
            requestBody = @RequestBody(content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "object", implementation = UploadForm.class)
            )),
            responses = {
                    @ApiResponse(responseCode = "201", description = "上传成功"),
                    @ApiResponse(responseCode = "400", description = "FILE_EMPTY(40002) / FILE_TOO_LARGE(40003) / UNSUPPORTED_FILE_TYPE(40004) / INVALID_FILE_NAME(40005)"),
                    @ApiResponse(responseCode = "500", description = "FILE_UPLOAD_FAILED(40006)")
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileMetadataResponse upload(@RequestParam("file") MultipartFile file) {
        return fileApplicationService.upload(file);
    }

    @Operation(summary = "下载文件", description = "按 ID 流式下载。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "下载成功",
                            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
                    @ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND(40001)"),
                    @ApiResponse(responseCode = "500", description = "FILE_DOWNLOAD_FAILED(40007)")
            })
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id,
                                                        HttpServletResponse response) {
        DownloadResource resource = fileApplicationService.download(id);
        String encodedName = URLEncoder.encode(resource.filename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        MediaType mediaType = parseMediaType(resource.metadata().getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.metadata().getSize()))
                .body(new InputStreamResource(resource.input()));
    }

    @Operation(summary = "查询文件元数据",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND(40001)")
            })
    @GetMapping("/{id}/info")
    public FileMetadataResponse info(@PathVariable Long id) {
        return fileApplicationService.getMetadata(id);
    }

    @Operation(summary = "删除文件（软删除）",
            description = "所有者或 admin 可删除；非所有者非 admin 返回 400 FILE_ACCESS_DENIED(40008)。",
            responses = {
                    @ApiResponse(responseCode = "204", description = "删除成功"),
                    @ApiResponse(responseCode = "400", description = "FILE_ACCESS_DENIED(40008)"),
                    @ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND(40001)")
            })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        fileApplicationService.delete(id);
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /** Swagger 用：multipart 上传表单结构 */
    @Schema(description = "上传表单")
    private record UploadForm(
            @Schema(type = "string", format = "binary", description = "待上传文件") MultipartFile file
    ) {
    }
}

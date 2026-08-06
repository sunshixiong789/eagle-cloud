package com.eagle.system.file.application.service;

import com.eagle.oss.service.StorageService;
import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.system.file.application.mapper.FileMapper;
import com.eagle.system.file.domain.model.aggregate.FileMetadata;
import com.eagle.system.file.domain.model.enums.FileErrorCode;
import com.eagle.system.file.domain.repository.FileMetadataRepository;
import com.eagle.system.file.infrastructure.config.FileStorageProperties;
import com.eagle.system.file.interfaces.dto.response.FileMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件应用服务
 *
 * <p>编排校验 + 持久化元数据 + 调用底层 {@link StorageService}。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileApplicationService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileMetadataRepository fileRepository;
    private final StorageService storageService;
    private final FileStorageProperties properties;
    private final FileMapper fileMapper;

    /**
     * 上传文件。
     *
     * @param file multipart 文件
     * @return 文件元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public FileMetadataResponse upload(MultipartFile file) {
        validateFile(file);

        String uploadedBy = currentUserId();
        String objectKey = buildObjectKey(file.getOriginalFilename(), uploadedBy);
        String bucket = properties.getBucket();
        String contentType = resolveContentType(file);

        try (InputStream in = file.getInputStream()) {
            storageService.upload(bucket, objectKey, in, file.getSize(), contentType);
        } catch (IOException ex) {
            log.error("file upload failed: bucket={}, key={}", bucket, objectKey, ex);
            throw FileErrorCode.FILE_UPLOAD_FAILED.toServiceException(ex);
        }

        FileMetadata metadata = FileMetadata.create(
                bucket,
                objectKey,
                file.getOriginalFilename(),
                file.getSize(),
                contentType,
                null,
                uploadedBy
        );
        FileMetadata saved = fileRepository.save(metadata);
        log.info("file uploaded: id={}, bucket={}, key={}, size={}, uploadedBy={}",
                saved.getId(), bucket, objectKey, file.getSize(), uploadedBy);
        return fileMapper.toResponse(saved);
    }

    /**
     * 获取文件下载输入流。调用方负责关闭流。
     *
     * @param id 文件 ID
     * @return 元数据 + 输入流
     */
    @Transactional(readOnly = true)
    public DownloadResource download(Long id) {
        FileMetadata metadata = loadOrThrow(id);
        InputStream input;
        try {
            input = storageService.download(metadata.getBucket(), metadata.getObjectKey());
        } catch (RuntimeException ex) {
            log.error("file download failed: id={}, bucket={}, key={}", id, metadata.getBucket(),
                    metadata.getObjectKey(), ex);
            throw FileErrorCode.FILE_DOWNLOAD_FAILED.toServiceException(ex);
        }
        return new DownloadResource(metadata, input);
    }

    /**
     * 查询文件元数据
     */
    @Transactional(readOnly = true)
    public FileMetadataResponse getMetadata(Long id) {
        return fileMapper.toResponse(loadOrThrow(id));
    }

    /**
     * 软删除文件：所有者或 admin。底层存储延后清理。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileMetadata metadata = loadOrThrow(id);
        if (!SecurityUtils.hasRole("admin") && !metadata.isOwnedBy(currentUserId())) {
            throw FileErrorCode.FILE_ACCESS_DENIED.toDomainException();
        }
        metadata.markDeleted();
        fileRepository.save(metadata);
        log.info("file soft-deleted: id={}, bucket={}, key={}", id, metadata.getBucket(),
                metadata.getObjectKey());
    }

    private FileMetadata loadOrThrow(Long id) {
        return fileRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(FileErrorCode.FILE_NOT_FOUND::toNotFoundException);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw FileErrorCode.FILE_EMPTY.toDomainException();
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw FileErrorCode.FILE_TOO_LARGE.toDomainException(properties.getMaxSizeMb());
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || containsPathTraversal(original)) {
            throw FileErrorCode.INVALID_FILE_NAME.toDomainException();
        }
        String ext = extensionOf(original).toLowerCase();
        Set<String> allowed = properties.getAllowedExtensionsLower();
        if (ext.isEmpty() || !allowed.contains(ext)) {
            throw FileErrorCode.UNSUPPORTED_FILE_TYPE.toDomainException(ext);
        }
    }

    private boolean containsPathTraversal(String name) {
        return name.contains("..") || name.contains("/") || name.contains("\\") || name.contains("\0");
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1 ? filename.substring(dot + 1) : "";
    }

    private String buildObjectKey(String originalName, String uploadedBy) {
        String ext = extensionOf(originalName != null ? originalName : "");
        String date = LocalDate.now().format(DATE_PATH);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ext.isEmpty()
                ? String.format("%s/%s/%s", uploadedBy, date, uuid)
                : String.format("%s/%s/%s.%s", uploadedBy, date, uuid, ext);
    }

    private String resolveContentType(MultipartFile file) {
        String type = file.getContentType();
        return (type == null || type.isBlank()) ? "application/octet-stream" : type;
    }

    private String currentUserId() {
        Long id = SecurityUtils.getCurrentUserId();
        if (id == null) {
            throw FileErrorCode.FILE_ACCESS_DENIED.toDomainException();
        }
        return id.toString();
    }

    /**
     * 下载用资源，调用方需关闭 {@link #input}
     */
    public record DownloadResource(FileMetadata metadata, InputStream input) {
        public String filename() {
            String original = metadata.getOriginalName();
            return original != null && !original.isBlank() ? original : "download";
        }
    }
}

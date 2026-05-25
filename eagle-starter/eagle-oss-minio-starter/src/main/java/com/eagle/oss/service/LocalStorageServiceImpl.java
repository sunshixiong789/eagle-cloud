package com.eagle.oss.service;

import com.eagle.common.exception.codes.FileErrorCode;
import com.eagle.oss.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件系统存储服务实现。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class LocalStorageServiceImpl implements StorageService {

    private final StorageProperties properties;

    @Override
    public String upload(String bucket, String path, InputStream input, long size, String mimeType) {
        Path target = resolvePath(bucket, path);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Saved local file: {}", target);
        } catch (IOException e) {
            throw FileErrorCode.FILE_UPLOAD_ERROR.toServiceException(e);
        }
        return properties.getLocal().getUrlPrefix() + "/" + bucket + "/" + path;
    }

    @Override
    public InputStream download(String bucket, String path) {
        Path target = resolvePath(bucket, path);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw FileErrorCode.FILE_NOT_FOUND.toNotFoundException();
        }
    }

    @Override
    public void delete(String bucket, String path) {
        Path target = resolvePath(bucket, path);
        try {
            Files.deleteIfExists(target);
            log.debug("Deleted local file: {}", target);
        } catch (IOException e) {
            throw FileErrorCode.FILE_DELETE_ERROR.toServiceException(e);
        }
    }

    @Override
    public String getUrl(String bucket, String path) {
        return properties.getLocal().getUrlPrefix() + "/" + bucket + "/" + path;
    }

    private Path resolvePath(String bucket, String path) {
        return Paths.get(properties.getLocal().getBasePath(), bucket, path);
    }
}

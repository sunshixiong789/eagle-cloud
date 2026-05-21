package com.eagle.example.integration.minio;

import com.eagle.oss.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文件存储 Starter 验证服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageVerificationService {

    private final StorageService storageService;

    @SneakyThrows
    public String uploadText(String bucket, String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String url = storageService.upload(bucket, path, new ByteArrayInputStream(bytes), bytes.length, "text/plain");
        log.info("Uploaded to: {}", url);
        return url;
    }

    @SneakyThrows
    public String downloadText(String bucket, String path) {
        try (var is = storageService.download(bucket, path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public void delete(String bucket, String path) {
        storageService.delete(bucket, path);
    }
}

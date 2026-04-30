package com.eagle.oss.service;

import com.eagle.oss.properties.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import com.eagle.common.exception.codes.FileErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 存储服务实现。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    @Override
    public String upload(String bucket, String path, InputStream input, long size, String mimeType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(input, size, -1)
                    .contentType(mimeType)
                    .build());
            log.debug("Uploaded to MinIO: {}/{}", bucket, path);
            return getUrl(bucket, path);
        } catch (Exception e) {
            throw FileErrorCode.FILE_UPLOAD_ERROR.toServiceException(e);
        }
    }

    @Override
    public InputStream download(String bucket, String path) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw FileErrorCode.FILE_NOT_FOUND.toNotFoundException();
        }
    }

    @Override
    public void delete(String bucket, String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
            log.debug("Deleted from MinIO: {}/{}", bucket, path);
        } catch (Exception e) {
            throw FileErrorCode.FILE_DELETE_ERROR.toServiceException(e);
        }
    }

    @Override
    public String getUrl(String bucket, String path) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw FileErrorCode.FILE_NOT_FOUND.toServiceException(e);
        }
    }
}

package com.eagle.system.file.domain.model.aggregate;

import com.eagle.datajpa.base.BaseAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 文件元数据聚合根
 * <p>
 * 持有文件在对象存储中的位置（bucket + objectKey）与业务侧的展示信息（原始名、大小、MIME）。
 * 业务表只引用 {@code id}，不直接拼接 OSS URL（满足 {@code rules/26-file-storage.md}）。
 *
 * <p>软删除：调用 {@link #markDeleted()} 标记后由定时任务异步清理底层对象。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "sys_file", comment = "文件元数据表",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_file_bucket_key", columnNames = {"bucket", "object_key"})
        },
        indexes = {
                @Index(name = "idx_file_uploaded_by", columnList = "uploaded_by")
        })
public class FileMetadata extends BaseAggregateRoot<FileMetadata> {

    @Column(name = "bucket", nullable = false, updatable = false, length = 64, comment = "存储桶")
    private String bucket;

    @Column(name = "object_key", nullable = false, updatable = false, length = 512,
            comment = "对象 Key（{uploadedBy}/{yyyy/MM/dd}/{uuid}.{ext}）")
    private String objectKey;

    @Column(name = "original_name", length = 255, comment = "上传时的原始文件名")
    private String originalName;

    @Column(name = "size", nullable = false, comment = "字节数")
    private Long size;

    @Column(name = "content_type", length = 128, comment = "MIME 类型")
    private String contentType;

    @Column(name = "md5", length = 32, comment = "文件 MD5（可选）")
    private String md5;

    @Column(name = "uploaded_by", length = 64, updatable = false, comment = "上传者用户 ID")
    private String uploadedBy;

    @Column(name = "deleted", nullable = false, comment = "软删除标记（0=可用，1=已删除）")
    private boolean deleted;

    private FileMetadata(String bucket, String objectKey, String originalName,
                         Long size, String contentType, String md5, String uploadedBy) {
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.originalName = originalName;
        this.size = size;
        this.contentType = contentType;
        this.md5 = md5;
        this.uploadedBy = uploadedBy;
        this.deleted = false;
    }

    public static FileMetadata create(String bucket, String objectKey,
                                      String originalName, long size, String contentType,
                                      String md5, String uploadedBy) {
        return new FileMetadata(bucket, objectKey, originalName, size,
                contentType, md5, uploadedBy);
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isOwnedBy(String userId) {
        return userId != null && userId.equals(this.uploadedBy);
    }
}

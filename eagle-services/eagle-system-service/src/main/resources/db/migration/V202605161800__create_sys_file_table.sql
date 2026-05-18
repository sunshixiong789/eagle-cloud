-- =====================================================================
-- V202605161800: sys_file（FileMetadata 聚合根）
--
-- 文件元数据持久化表，对象本体由 eagle-oss-minio-starter 存到底层（local / minio）。
-- 业务表只引用 id，不直接拼接 OSS URL（rules/26-file-storage.md）。
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version       BIGINT                                COMMENT '乐观锁版本号',
    create_by     BIGINT                                COMMENT '创建人 ID',
    create_time   TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by     BIGINT                                COMMENT '更新人 ID',
    update_time   TIMESTAMP                             COMMENT '更新时间',
    tenant_id     VARCHAR(64)  NOT NULL                 COMMENT '租户 ID（单租户部署默认 default）',
    bucket        VARCHAR(64)  NOT NULL                 COMMENT '存储桶',
    object_key    VARCHAR(512) NOT NULL                 COMMENT '对象 Key（{tenant}/{uploader}/{yyyy/MM/dd}/{uuid}.{ext}）',
    original_name VARCHAR(255)                          COMMENT '上传时的原始文件名',
    size          BIGINT       NOT NULL                 COMMENT '字节数',
    content_type  VARCHAR(128)                          COMMENT 'MIME 类型',
    md5           CHAR(32)                              COMMENT '文件 MD5（可选）',
    uploaded_by   VARCHAR(64)                           COMMENT '上传者用户 ID',
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE   COMMENT '软删除标记（0=可用，1=已删除）',
    PRIMARY KEY (id),
    CONSTRAINT uk_file_bucket_key UNIQUE (bucket, object_key)
);
CREATE INDEX IF NOT EXISTS idx_file_tenant_uploader ON sys_file (tenant_id, uploaded_by);
CREATE INDEX IF NOT EXISTS idx_file_uploaded_by     ON sys_file (uploaded_by);

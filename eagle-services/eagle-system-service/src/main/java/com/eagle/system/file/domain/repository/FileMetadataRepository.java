package com.eagle.system.file.domain.repository;

import com.eagle.system.file.domain.model.aggregate.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 文件元数据 Repository
 *
 * @author sunshixiong
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * 按 id 查询未软删除的文件
     */
    Optional<FileMetadata> findByIdAndDeletedFalse(Long id);
}

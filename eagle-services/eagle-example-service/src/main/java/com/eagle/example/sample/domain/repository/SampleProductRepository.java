package com.eagle.example.sample.domain.repository;

import com.eagle.example.sample.domain.model.SampleProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 商品仓储接口。
 */
@Repository
public interface SampleProductRepository extends JpaRepository<SampleProduct, Long>, JpaSpecificationExecutor<SampleProduct> {

    Optional<SampleProduct> findByName(String name);

    boolean existsByName(String name);

    Page<SampleProduct> findAllByEnabledTrue(Pageable pageable);
}

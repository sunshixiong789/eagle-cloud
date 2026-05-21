package com.eagle.example.sample.application.service;

import com.eagle.audit.annotation.AuditLog;
import com.eagle.example.sample.application.command.CreateProductCommand;
import com.eagle.example.sample.application.command.UpdateProductCommand;
import com.eagle.example.sample.application.dto.ProductDto;
import com.eagle.example.sample.application.mapper.ProductMapper;
import com.eagle.example.sample.domain.SampleErrorCode;
import com.eagle.example.sample.domain.model.SampleProduct;
import com.eagle.example.sample.domain.repository.SampleProductRepository;
import com.eagle.idempotency.annotation.Idempotent;
import com.eagle.redis.util.RedisRateLimiter;
import com.eagle.sentinel.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品应用服务。
 *
 * <p>演示注解：
 * <ul>
 *   <li>{@code @AuditLog} — 审计日志（eagle-audit-log-starter）</li>
 *   <li>{@code @Idempotent} — 接口幂等（eagle-idempotency-starter）</li>
 *   <li>{@code @RateLimit} — Sentinel 限流（eagle-sentinel-starter）</li>
 *   <li>{@code @Cacheable/@CachePut/@CacheEvict} — 多级缓存（eagle-redis-starter）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SampleProductApplicationService {

    private final SampleProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RedisRateLimiter redisRateLimiter;

    @AuditLog(module = "示例商品", action = "创建商品")
    @Idempotent(mode = com.eagle.idempotency.annotation.IdempotencyMode.BUSINESS_KEY, key = "#command.name()")
    @RateLimit(resource = "createProduct", qps = 100)
    @Transactional
    @CacheEvict(value = "productList", allEntries = true)
    public ProductDto create(CreateProductCommand command) {
        if (productRepository.existsByName(command.name())) {
            throw SampleErrorCode.PRODUCT_NAME_EXISTS.toConflictException();
        }
        SampleProduct product = productMapper.toEntity(command);
        SampleProduct saved = productRepository.save(product);
        log.info("Product created: id={}, name={}", saved.getId(), saved.getName());
        return productMapper.toDto(saved);
    }

    @AuditLog(module = "示例商品", action = "更新商品")
    @RateLimit(resource = "updateProduct", qps = 100)
    @Transactional
    @CacheEvict(value = {"product", "productList"}, allEntries = true)
    public ProductDto update(UpdateProductCommand command) {
        SampleProduct product = productRepository.findById(command.id())
                .orElseThrow(() -> SampleErrorCode.PRODUCT_NOT_FOUND.toNotFoundException());
        product.update(command.name(), command.price(), command.description(), command.supplierPhone());
        return productMapper.toDto(product);
    }

    @AuditLog(module = "示例商品", action = "删除商品")
    @Transactional
    @CacheEvict(value = {"product", "productList"}, allEntries = true)
    public void delete(Long id) {
        SampleProduct product = productRepository.findById(id)
                .orElseThrow(() -> SampleErrorCode.PRODUCT_NOT_FOUND.toNotFoundException());
        productRepository.delete(product);
    }

    @Cacheable(value = "product", key = "#id")
    public ProductDto findById(Long id) {
        SampleProduct product = productRepository.findById(id)
                .orElseThrow(() -> SampleErrorCode.PRODUCT_NOT_FOUND.toNotFoundException());
        return productMapper.toDto(product);
    }

    @Cacheable(value = "productList")
    public Page<ProductDto> findAll(Pageable pageable) {
        return productRepository.findAllByEnabledTrue(pageable)
                .map(productMapper::toDto);
    }

    /**
     * 演示 Redis 限流。
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        return redisRateLimiter.tryAcquire(key, maxRequests, windowSeconds);
    }

    /**
     * 批量创建（演示事务）。
     */
    @Transactional
    @CacheEvict(value = "productList", allEntries = true)
    public List<ProductDto> batchCreate(List<CreateProductCommand> commands) {
        return commands.stream()
                .map(this::create)
                .toList();
    }
}

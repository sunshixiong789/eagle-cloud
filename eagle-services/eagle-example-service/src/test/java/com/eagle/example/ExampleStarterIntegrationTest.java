package com.eagle.example;

import com.eagle.example.integration.cache.RedisVerificationService;
import com.eagle.example.integration.idgenerator.IdGeneratorVerificationService;
import com.eagle.example.integration.minio.StorageVerificationService;
import com.eagle.example.integration.mybatis.SampleProductMyBatisMapper;
import com.eagle.example.sample.application.service.SampleProductApplicationService;
import com.eagle.example.sample.domain.model.SampleProduct;
import com.eagle.example.sample.domain.repository.SampleProductRepository;
import com.eagle.excel.writer.ExcelWriter;
import com.eagle.http.client.support.EagleHttpServiceClientFactory;
import com.eagle.idgenerator.util.IdGeneratorFacade;
import com.eagle.message.service.NotificationService;
import com.eagle.oss.service.StorageService;
import com.eagle.redis.lock.RedisDistributedLock;
import com.eagle.redis.util.CacheProtectionUtil;
import com.eagle.redis.util.RedisRateLimiter;
import com.eagle.redis.util.RedissonBloomFilterUtil;
import com.eagle.websocket.session.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Eagle Starter 全量集成测试。
 *
 * <p>验证各 starter 的 Bean 能正确注入，核心功能可调用。
 */
@SpringBootTest(classes = EagleExampleApplication.class)
class ExampleStarterIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ExampleStarterIntegrationTest.class);

    @Autowired
    private SampleProductRepository productRepository;

    @Autowired
    private SampleProductApplicationService productService;

    @Autowired
    private SampleProductMyBatisMapper myBatisMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private RedisDistributedLock redisDistributedLock;

    @Autowired(required = false)
    private RedisRateLimiter redisRateLimiter;

    @Autowired(required = false)
    private CacheProtectionUtil cacheProtectionUtil;

    @Autowired(required = false)
    private RedissonBloomFilterUtil bloomFilterUtil;

    @Autowired
    private IdGeneratorFacade idGeneratorFacade;

    @Autowired(required = false)
    private StorageService storageService;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private WebSocketSessionManager webSocketSessionManager;

    @Autowired
    private ExcelWriter excelWriter;

    @Autowired
    private EagleHttpServiceClientFactory httpClientFactory;

    // ==================== 上下文加载 ====================

    @Test
    void contextLoads() {
        log.info("Spring context loaded successfully");
    }

    // ==================== JPA / Data ====================

    @Test
    void jpaCrud() {
        SampleProduct product = SampleProduct.create(
                "Test Product", new BigDecimal("99.99"), 100,
                "Test Description", "13800138000");
        SampleProduct saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Product");

        SampleProduct found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));

        productRepository.deleteById(saved.getId());
        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void applicationService_shouldCreateProduct() {
        var command = new com.eagle.example.sample.application.command.CreateProductCommand(
                "Service Test", new BigDecimal("19.99"), 50, "desc", null);
        var dto = productService.create(command);
        assertThat(dto.id()).isNotNull();
        assertThat(dto.name()).isEqualTo("Service Test");
    }

    // ==================== MyBatis ====================

    @Test
    void myBatisMapper_shouldBeInjected() {
        assertThat(myBatisMapper).isNotNull();
    }

    // ==================== Redis ====================

    @Test
    void redisTemplate_shouldBeInjected() {
        assertThat(stringRedisTemplate).isNotNull();
    }

    @Test
    void redisBeans_shouldBeInjected() {
        assertThat(redisDistributedLock).isNotNull();
        assertThat(redisRateLimiter).isNotNull();
        assertThat(cacheProtectionUtil).isNotNull();
        assertThat(bloomFilterUtil).isNotNull();
    }

    // ==================== ID Generator ====================

    @Test
    void idGenerator_shouldGenerateIds() {
        long snowflake = idGeneratorFacade.snowflakeId();
        assertThat(snowflake).isPositive();

        String uuid = idGeneratorFacade.uuid();
        assertThat(uuid).isNotBlank();

        String tsid = idGeneratorFacade.tsidStr();
        assertThat(tsid).isNotBlank();

        String nanoId = idGeneratorFacade.nanoId(8);
        assertThat(nanoId).hasSize(8);

        String orderNo = idGeneratorFacade.orderNo("TEST");
        assertThat(orderNo).startsWith("TEST");
    }

    // ==================== Storage ====================

    @Test
    void storageService_shouldBeInjected() {
        assertThat(storageService).isNotNull();
    }

    // ==================== Excel ====================

    @Test
    void excelWriter_shouldBeInjected() {
        assertThat(excelWriter).isNotNull();
    }

    // ==================== HTTP Client ====================

    @Test
    void httpClientFactory_shouldBeInjected() {
        assertThat(httpClientFactory).isNotNull();
    }
}

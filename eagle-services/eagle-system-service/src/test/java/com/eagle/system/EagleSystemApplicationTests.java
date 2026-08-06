package com.eagle.system;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * 完整 Spring 上下文启动烟雾测试。
 *
 * <p>默认 {@link Disabled}：启动该测试会装载所有业务 Bean —— OnlineUserAdapter 等
 * 依赖 StringRedisTemplate / EntityManager 等外部资源，没有真实 PostgreSQL+Redis 起不来。
 * 与"单元测试不依赖外部资源"原则冲突。业务逻辑覆盖由同模块下纯 Mockito 单测
 * （{@code *Test.java}）保证。
 *
 * <p>需要做完整启动验证时（升级 Spring Boot、调整自动配置、排查 Bean 冲突），先用
 * Testcontainers 或本地 docker 起 PostgreSQL + Redis + Consul，再去掉 {@code @Disabled} 跑这个。
 *
 * @author sunshixiong
 */
@Disabled("启动完整 Spring 上下文需 DB/Redis/Consul；Mockito 单测已覆盖业务逻辑，仅在做集成冒烟时手工启用")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EagleSystemApplicationTests {

    @Test
    @DisplayName("上下文应能正常加载")
    void contextLoads() {

    }

}

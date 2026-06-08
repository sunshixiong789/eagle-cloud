package com.eagle.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EagleGatewayApplicationTests {

    @Test
    @DisplayName("上下文应能正常加载")
    void contextLoads() {
    }

}

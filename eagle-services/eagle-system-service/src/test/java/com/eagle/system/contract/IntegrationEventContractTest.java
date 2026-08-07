package com.eagle.system.contract;

import com.eagle.system.base.application.event.AccountDeletedMessage;
import com.eagle.system.base.application.event.AccountRegisteredMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成事件契约 —— <b>消费方</b>侧。
 * <p>
 * 按 {@code 02-architecture.md},本模块<b>禁止</b> import auth-service 的 {@code XxxIntegrationEvent},
 * 两侧靠 <b>JSON 字段名</b> 对齐。本测试读取生产方落盘的 {@code docs/contracts/*.json},
 * 反序列化成本模块的 {@code XxxMessage},断言消费方真正用到的字段都能填上。
 * <p>
 * 生产方一旦改字段名(如 {@code phone} → {@code mobile}),契约文件随之变化,
 * 这里的断言立刻失败 —— 补上了"字段名即契约"留下的风险敞口。
 *
 * <p><strong>运行</strong>
 * <pre>gradle :eagle-services:eagle-system-service:test --tests "*IntegrationEventContractTest"</pre>
 */
@DisplayName("集成事件契约(消费方)")
class IntegrationEventContractTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static Path repoRoot() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("settings.gradle"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("未找到仓库根(settings.gradle)");
        }
        return p;
    }

    private <T> T readContract(String name, Class<T> type) throws IOException {
        Path file = repoRoot().resolve("docs/contracts").resolve(name + ".json");
        assertThat(file)
                .as("契约文件缺失,请先跑 auth-service 的 IntegrationEventContractTest 生成 %s.json", name)
                .exists();
        return MAPPER.readValue(Files.readString(file), type);
    }

    @Test
    @DisplayName("AccountRegistered:生产方字段能完整填充消费方 Message")
    void shouldDeserializeAccountRegistered() throws IOException {
        AccountRegisteredMessage msg = readContract("AccountRegistered", AccountRegisteredMessage.class);

        // 这些字段是本模块创建用户时真正读取的,任一为 null 都说明契约已被破坏
        assertThat(msg.getAccountId()).as("accountId").isNotNull();
        assertThat(msg.getUsername()).as("username").isNotBlank();
        assertThat(msg.getPhone()).as("phone").isNotBlank();
        assertThat(msg.getNickname()).as("nickname").isNotBlank();
        assertThat(msg.getEmail()).as("email").isNotBlank();
        assertThat(msg.getEventVersion()).as("eventVersion 用于协议版本判断").isNotBlank();
        assertThat(msg.getEventId()).as("eventId 是幂等 key").isNotBlank();
    }

    @Test
    @DisplayName("AccountDeleted:生产方字段能完整填充消费方 Message")
    void shouldDeserializeAccountDeleted() throws IOException {
        AccountDeletedMessage msg = readContract("AccountDeleted", AccountDeletedMessage.class);

        assertThat(msg.getAccountId()).as("accountId").isNotNull();
        assertThat(msg.getEventVersion()).as("eventVersion").isNotBlank();
        assertThat(msg.getEventId()).as("eventId 是幂等 key").isNotBlank();
    }
}

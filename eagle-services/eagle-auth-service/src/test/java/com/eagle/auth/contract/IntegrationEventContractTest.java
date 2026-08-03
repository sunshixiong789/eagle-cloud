package com.eagle.auth.contract;

import com.eagle.auth.core.infrastructure.event.integration.AccountDeletedIntegrationEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountPhoneChangedIntegrationEvent;
import com.eagle.auth.core.infrastructure.event.integration.AccountRegisteredIntegrationEvent;
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
 * 集成事件契约 —— <b>生产方</b>侧。
 * <p>
 * {@code 02-architecture.md} 规定:消费方独立声明 {@code XxxMessage},<b>禁止</b> import 生产方类,
 * <b>字段名是唯一契约</b>。这条规则带来一个风险敞口:生产方改字段名时,消费方不会红。
 * <p>
 * 本测试把每个集成事件的样例序列化后写入 {@code docs/contracts/},并提交进版本库:
 * <ul>
 *   <li>生产方改字段名 → 契约文件 diff 可见,评审时拦得住</li>
 *   <li>消费方侧 {@code IntegrationEventContractTest} 读同一份文件反序列化,字段对不上即失败</li>
 * </ul>
 * 这是不引入 Pact / Spring Cloud Contract 的最轻量做法。
 *
 * <p><strong>运行</strong>
 * <pre>gradle :eagle-services:eagle-auth-service:test --tests "*IntegrationEventContractTest"</pre>
 */
@DisplayName("集成事件契约(生产方)")
class IntegrationEventContractTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    /** 从测试工作目录向上找到仓库根(以 settings.gradle 为标记)。 */
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

    private void writeContract(String name, Object event) throws IOException {
        Path dir = repoRoot().resolve("docs/contracts");
        Files.createDirectories(dir);

        // eventId / occurredOn 每次运行都不同,若原样落盘会产生无意义的 git diff,
        // 淹没真正的契约变更。归一化为固定值,让 diff 只反映"字段增删改名"。
        var node = MAPPER.valueToTree(event);
        if (node.isObject()) {
            var obj = (tools.jackson.databind.node.ObjectNode) node;
            if (obj.has("eventId")) {
                obj.put("eventId", "00000000-0000-0000-0000-000000000000");
            }
            if (obj.has("occurredOn")) {
                obj.put("occurredOn", "2000-01-01T00:00:00");
            }
        }
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        Files.writeString(dir.resolve(name + ".json"), json + System.lineSeparator());

        // 契约必须含 eventVersion —— 消费方靠它区分协议版本
        assertThat(json).contains("\"eventVersion\"");
    }

    @Test
    @DisplayName("AccountRegistered 契约样例落盘")
    void shouldPublishAccountRegisteredContract() throws IOException {
        writeContract("AccountRegistered", new AccountRegisteredIntegrationEvent(
                1001L, "alice", "13800001234", "Alice", "https://cdn/a.png", "alice@example.com"));
    }

    @Test
    @DisplayName("AccountDeleted 契约样例落盘")
    void shouldPublishAccountDeletedContract() throws IOException {
        writeContract("AccountDeleted", new AccountDeletedIntegrationEvent(1001L));
    }

    @Test
    @DisplayName("AccountPhoneChanged 契约样例落盘")
    void shouldPublishAccountPhoneChangedContract() throws IOException {
        writeContract("AccountPhoneChanged",
                new AccountPhoneChangedIntegrationEvent(1001L, "13900005678"));
    }
}

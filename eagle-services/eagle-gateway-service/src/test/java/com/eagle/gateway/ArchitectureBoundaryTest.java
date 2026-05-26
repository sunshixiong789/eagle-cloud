package com.eagle.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * eagle-gateway-service 架构边界验证(文件扫描)。
 *
 * <p>网关只负责路由 / 限流 / 鉴权透传,业务零侵入。本测试通过文件扫描禁止
 * 网关代码 import 任何业务服务({@code com.eagle.auth} /
 * {@code com.eagle.system} / {@code com.eagle.monolith}),
 * 防止"图省事"在网关里直接调用业务领域对象。
 *
 * <p>网关与业务服务的合法交互通道只有:
 * <ul>
 *   <li>HTTP 路由配置(yml / Nacos)</li>
 *   <li>JWT claim(透传到下游)</li>
 *   <li>Sentinel 网关规则</li>
 * </ul>
 *
 * @author sunshixiong
 */
@DisplayName("Eagle Gateway 架构边界验证")
class ArchitectureBoundaryTest {

    private static final Path SRC_MAIN = Paths.get("src/main/java");

    private static final List<Pattern> FORBIDDEN_IMPORTS = List.of(
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.auth\\..*;"),
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.system\\..*;"),
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.monolith\\..*;")
    );

    @Test
    @DisplayName("gateway-service 不应 import 任何业务服务(auth / system / monolith)的代码")
    void shouldNotImportBusinessServicePackages() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(SRC_MAIN)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .forEach(file -> scanFile(file, violations));
        }
        if (!violations.isEmpty()) {
            fail("发现非法跨服务 import:\n  - " + String.join("\n  - ", violations));
        }
    }

    @Test
    @DisplayName("源码根目录应存在")
    void srcMainShouldExist() {
        assertTrue(Files.isDirectory(SRC_MAIN),
                "src/main/java 不存在,工作目录是否正确? cwd="
                        + Paths.get("").toAbsolutePath());
    }

    private static void scanFile(Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                for (Pattern pattern : FORBIDDEN_IMPORTS) {
                    if (pattern.matcher(line).matches()) {
                        violations.add(file + ":" + (i + 1) + " → " + line);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to read " + file, e);
        }
    }
}

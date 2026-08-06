#!/usr/bin/env bash
# check-java-conventions.sh — PreToolUse 拦截：写 Java 前挡掉规则明令禁止的写法
#
# 只做**确定性**检查（正则可判定、零歧义），需要推理的留给规则文本 + 人工评审。
# 命中 → exit 2，stderr 回喂给模型自行改正。
#
# 输入：stdin 的 hook JSON（tool_name / tool_input.file_path / tool_input.content|new_string）
# 退出：0 放行；2 阻断

set -uo pipefail

input=$(cat)

# jq 缺失时静默放行 —— 绝不能因为工具链问题阻塞开发
command -v jq >/dev/null 2>&1 || exit 0

file_path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')
[ -z "$file_path" ] && exit 0

# 只管 Java 主代码；测试与生成代码放行
case "$file_path" in
  *.java) ;;
  *) exit 0 ;;
esac
case "$file_path" in
  */build/*|*/generated/*|*/src/test/*) exit 0 ;;
esac

# Write 用 content，Edit 用 new_string
content=$(printf '%s' "$input" | jq -r '.tool_input.content // .tool_input.new_string // empty')
[ -z "$content" ] && exit 0

VIOLATIONS=""
add() { VIOLATIONS="${VIOLATIONS}  ✗ $1\n     → $2\n"; }

# --- 00-core.md：配置注入 ---
if printf '%s' "$content" | grep -qE '@Value\s*\(\s*"\$\{'; then
  add "使用了 @Value 注入配置" \
      "改用 @ConfigurationProperties（前缀 eagle.{feature}，Properties 类放 infrastructure/config/）。见 rules/00-core.md"
fi

# --- 06-boot4.md：Jackson 3 分包（注解仍在 com.fasterxml，必须放行） ---
if printf '%s' "$content" | grep -qE 'import\s+com\.fasterxml\.jackson\.(databind|core)\.'; then
  add "使用了 Jackson 2 的 databind/core 包" \
      "本仓库是 Jackson 3：核心类在 tools.jackson.*（注解仍在 com.fasterxml.jackson.annotation.*，那个是对的）。见 rules/06-boot4.md"
fi

# --- 03-api-error.md：多余的权限注解 ---
if printf '%s' "$content" | grep -qE '@PreAuthorize\s*\(\s*"isAuthenticated\(\)"'; then
  add "使用了 @PreAuthorize(\"isAuthenticated()\")" \
      "filter chain 已是 anyRequest().authenticated()，此注解多余。直接删掉。见 rules/03-api-error.md"
fi
if printf '%s' "$content" | grep -qE '@PreAuthorize\s*\(\s*"permitAll\(\)"'; then
  add "使用了 @PreAuthorize(\"permitAll()\")" \
      "该注解不会让接口变公开，路径不在白名单仍 401。公开接口请加进 yml 的 eagle.resource-server.permit-paths。见 rules/03-api-error.md"
fi

# --- 04-data.md：物理外键（@ForeignKey 常换行，需跨行判断） ---
if printf '%s' "$content" | perl -0777 -ne 'exit 1 if /\@JoinColumn\b/ && do { my $bad=0; while (/\@JoinColumn\b/g) { $bad=1 unless substr($_, pos(), 200) =~ /NO_CONSTRAINT/ } $bad }'; then :; else
  add "@JoinColumn 未禁用物理外键" \
      "补 foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)。本仓库所有表禁止物理外键。见 rules/04-data.md"
fi

# --- 04-data.md：表名前缀 ---
if printf '%s' "$content" | grep -qE '@Table\s*\(\s*name\s*=\s*"t_'; then
  add "表名用了 t_ 前缀" \
      "本仓库按服务域前缀：sys_ / auth_ / user_ / eagle_。见 rules/04-data.md"
fi

# --- 02-architecture.md：反射式拷贝 ---
if printf '%s' "$content" | grep -q 'BeanUtils\.copyProperties'; then
  add "使用了 BeanUtils.copyProperties" \
      "禁止反射式映射。用 record 静态工厂或 application/mapper/ 下的 @Component Mapper 逐字段显式映射。见 rules/02-architecture.md"
fi

# --- 00-core.md：JPA 实体禁 @Data / @Builder ---
if printf '%s' "$content" | grep -q '@Entity' && printf '%s' "$content" | grep -qE '^\s*@(Data|Builder)\b'; then
  add "JPA 实体标注了 @Data 或 @Builder" \
      "聚合根用 @Getter @NoArgsConstructor + 静态工厂 + 业务方法改状态，不暴露 setter。见 rules/00-core.md"
fi

# --- 01-java25.md：未开 preview，不得用 Structured Concurrency ---
if printf '%s' "$content" | grep -q 'StructuredTaskScope'; then
  add "使用了 Structured Concurrency" \
      "Java 25 中它仍是 preview，本项目未开 --enable-preview，编译会失败。见 rules/01-java25.md"
fi

# --- 01-java25.md / 08-quality.md：DTO 必须是 record（存量 45 处 @Data class，止血用） ---
case "$file_path" in
  */interfaces/dto/*)
    if printf '%s' "$content" | grep -qE '^\s*@(Data|Builder|Setter)\b'; then
      add "DTO 用了 @Data / @Builder / @Setter" \
          "请求/响应 DTO 一律 record，不加 Lombok。存量 45 个 @Data class 是待偿技术债，不要照抄。见 rules/01-java25.md、rules/08-quality.md"
    fi
    ;;
esac

# --- 06-boot4.md：starter 禁止 eagle.xxx.enabled 总开关 ---
if printf '%s' "$content" | grep -qE '@ConditionalOnProperty[^)]*"eagle\.[a-z0-9.-]*enabled"'; then
  add "使用了 eagle.xxx.enabled 总开关" \
      "starter 引入即生效，依赖坐标本身就是开关。条件注解只用于「选哪种实现」（mode / provider / 是否配了 url），不用于「要不要装」。见 rules/06-boot4.md"
fi

# --- 07-checklist.md 陷阱 23：引用已随 9 个 starter 移除的能力 ---
if printf '%s' "$content" | grep -qE '\bTenantContextHolder\b|@TenantFilter\b'; then
  add "引用了已移除的多租户 API" \
      "eagle-tenant-starter 已清空并移出 settings.gradle，TenantContextHolder / @TenantFilter 均不存在。新表也不要加 tenant_id。见 rules/05-security.md"
fi
if printf '%s' "$content" | grep -qE '\bAbstractRocketMqListener\b|@RocketMQMessageListener\b|import org\.apache\.rocketmq'; then
  add "引用了已移除的 RocketMQ API" \
      "消息中间件已换成 RabbitMQ：继承 AbstractAmqpListener<T>，构造器显式 super(amqpProperties)，并实现 getConsumerGroup()。见 rules/02-architecture.md"
fi
if printf '%s' "$content" | grep -qE '@GlobalTransactional\b|import io\.seata\.|import com\.alibaba\.csp\.sentinel|import com\.alibaba\.cloud\.nacos'; then
  add "引用了已移除的 Seata / Sentinel / Nacos" \
      "无分布式事务（走本地事务 + AFTER_COMMIT 集成事件 + 消费方幂等）；限流用 eagle-resilience 的 @RateLimit 或 RedisRateLimiter；注册中心是 Consul。见 rules/07-checklist.md 陷阱 23"
fi

if [ -n "$VIOLATIONS" ]; then
  {
    printf 'Eagle 规范检查未通过（%s）：\n\n' "$(basename "$file_path")"
    printf '%b' "$VIOLATIONS"
    printf '\n请修正后重试。完整规则见 .claude/rules/。\n'
  } >&2
  exit 2
fi

exit 0

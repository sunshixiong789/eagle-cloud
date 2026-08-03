#!/usr/bin/env bash
# sync.sh — 从 eagle-cloud 仓库源生成 plugin 内容
# 运行：在仓库根目录执行 ./agent-plugin/scripts/sync.sh
#
# rules/ 与 commands/ 是 plugin 内的源（直接在 agent-plugin/ 里编辑），脚本不再重写。
# 仅 skills/ 是从 eagle-starter/*/USAGE.md 自动生成（每个 starter 一个 SKILL.md）。

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PLUGIN_DIR="$ROOT_DIR/agent-plugin"

echo "==> 检查 rules/（源就地维护，无需同步）"
echo "    $(ls "$PLUGIN_DIR/rules" 2>/dev/null | wc -l | tr -d ' ') files"

echo "==> 检查 commands/（源就地维护，无需同步）"
echo "    $(ls "$PLUGIN_DIR/commands" 2>/dev/null | wc -l | tr -d ' ') files"

echo "==> 生成 skills/{starter}/SKILL.md（仅 starter 派生 skill，手写 skill 如 eagle-feature-flow 不受影响）"
mkdir -p "$PLUGIN_DIR/skills"

# 每个 starter 的触发关键词（用于 skill description，便于 AI 自动加载）
declare -a TRIGGERS

# (skill_name, source_starter, description)
add_skill() {
    local skill_name="$1"
    local starter_dir="$2"
    local description="$3"

    local source_file="$ROOT_DIR/eagle-starter/$starter_dir/USAGE.md"
    if [[ ! -f "$source_file" ]]; then
        echo "    SKIP: $source_file not found"
        return
    fi

    local target_dir="$PLUGIN_DIR/skills/$skill_name"
    mkdir -p "$target_dir/agents"

    # SKILL.md：frontmatter + USAGE.md 正文（Claude Code / Codex 都读这个）
    {
        printf -- '---\n'
        printf -- 'name: %s\n' "$skill_name"
        printf -- 'description: %s\n' "$description"
        printf -- '---\n\n'
        cat "$source_file"
    } > "$target_dir/SKILL.md"

    # agents/openai.yaml：Codex marketplace 展示用（display_name + short_description）
    # display_name 从 skill_name 转 Title Case（eagle-redis → Eagle Redis）
    local display
    display=$(echo "$skill_name" | awk -F'-' '{for(i=1;i<=NF;i++){printf "%s%s",toupper(substr($i,1,1)),substr($i,2);if(i<NF)printf " "}}')
    # short_description 取 description 的前 80 字符
    local short="${description:0:80}"
    {
        printf -- 'interface:\n'
        printf -- '  display_name: "%s"\n' "$display"
        printf -- '  short_description: "%s"\n' "$short"
    } > "$target_dir/agents/openai.yaml"

    echo "    ✅ $skill_name"
}

add_skill "eagle-common"              "eagle-common-starter" \
    "Use when working with DDD base classes (BaseAggregateRoot, BaseEntity), AppException/ErrorCode hierarchy, BaseEvent, DistributedLock interface, MessageSourceUtil, BusinessMetrics, EagleUser, or AsyncConfig taskExecutor in eagle-cloud projects"

add_skill "eagle-data-jpa"            "eagle-data-jpa-starter" \
    "Use when working with JPA/Hibernate in eagle-cloud projects — entity mapping, Spring Data Repository, JPA Auditing (createBy/createTime auto-fill), batch writes, slow query thresholds, EntityGraph"

add_skill "eagle-mybatis"             "eagle-mybatis-starter" \
    "Use when working with MyBatis-Plus in eagle-cloud projects — IEagleService/EagleServiceImpl base classes, BaseMapperPlus, EaglePageQuery/EaglePageResult unified pagination, slow SQL interceptor"

add_skill "eagle-dynamic-datasource"  "eagle-dynamic-datasource-starter" \
    "Use when implementing master/slave read-write splitting in eagle-cloud projects — @ReadOnly annotation, DataSourceContextHolder programmatic switching"

add_skill "eagle-elasticsearch"       "eagle-elasticsearch-starter" \
    "Use when working with Elasticsearch in eagle-cloud projects — full-text search, EsQueryBuilder fluent API, BaseElasticSearchRepository, highlighting, aggregations, EagleDocument base"

add_skill "eagle-id-generator"        "eagle-id-generator-starter" \
    "Use when generating distributed IDs in eagle-cloud projects — Snowflake, UUID v7, TSID, NanoId, business order numbers (orderNo/payNo/refundNo) via IdGeneratorFacade or IdGeneratorUtil static helpers"

add_skill "eagle-idempotency"         "eagle-idempotency-starter" \
    "Use when implementing API idempotency in eagle-cloud projects — @Idempotent annotation, TOKEN/BUSINESS_KEY/RESULT_CACHE modes, IdempotencyKeyExtractor"

add_skill "eagle-redis"               "eagle-redis-starter" \
    "Use when working with Redis in eagle-cloud projects — Spring caching with @Cacheable, distributed locks (DistributedLock/Redisson), CacheProtectionUtil getWithMutex (4 params with Class type), bloom filter, rate limiting (token bucket / sliding window), atomic counters with CAS, delayed queue, Pub/Sub topic"

add_skill "eagle-rocketmq"            "eagle-rocketmq-starter" \
    "Use when working with RocketMQ 5.x in eagle-cloud projects — DomainEventPublisher (publish/publishAsync/publishDelayed/publishOrdered), TransactionalEventPublisher, AbstractRocketMqListener (extend with getTopic/getEventClass/handle, NOT @RocketMQMessageListener), AbstractDlqListener, AbstractRocketMqTransactionChecker. Uses native 5.x MessageView, not 4.x MessageExt"

add_skill "eagle-resource-server"     "eagle-resource-server-starter" \
    "Use when implementing OAuth2 resource server (JWT-protected service) in eagle-cloud projects — @EnableEagleResourceServer, EagleAuthentication, SecurityUtils (getCurrentUser/getCurrentUserId/hasRole/hasAnyRole), @PreAuthorize, EagleUser principal"

add_skill "eagle-restclient"          "eagle-restclient-starter" \
    "Use when implementing service-to-service HTTP clients in eagle-cloud projects — Spring RestClient / HTTP Service Interface, automatic JWT/tenant-id/Seata-XID propagation, EagleResponseErrorHandler converting downstream HTTP errors to AppException hierarchy"

add_skill "eagle-tracing"             "eagle-tracing-starter" \
    "Use when configuring distributed tracing in eagle-cloud projects — Brave/B3/Zipkin integration, traceId/spanId MDC injection for log correlation, sampling probability"

add_skill "eagle-tenant"              "eagle-tenant-starter" \
    "Use when implementing multi-tenancy in eagle-cloud projects — TenantContextHolder (getTenantId/setTenantId/clear, NOT getCurrentTenantId), TenantAware interface, @TenantFilter on Service/Repository (NOT entity), Hibernate @FilterDef/@Filter on entities, COLUMN vs DATABASE mode"

add_skill "eagle-row-security"        "eagle-row-security-starter" \
    "Use when implementing row-level data permissions in eagle-cloud projects — @DataPermission(deptField, userField), DataScope enum (ALL/SELF/DEPT/DEPT_AND_CHILD/CUSTOM), DataPermissionProvider business implementation, JPA Specification injection"

add_skill "eagle-openapi"             "eagle-openapi-starter" \
    "Use when configuring OpenAPI/Swagger documentation in eagle-cloud projects — SpringDoc annotations (@Tag/@Operation/@Schema/@ApiResponses), grouping, JWT security scheme"

add_skill "eagle-oss-minio"           "eagle-oss-minio-starter" \
    "Use when implementing object storage (MinIO/local) in eagle-cloud projects — StorageService interface (upload/download/delete/getUrl), bucket/object key design, file upload validation"

add_skill "eagle-notification"        "eagle-notification-starter" \
    "Use when sending notifications (SMS/Email/in-app) in eagle-cloud projects — NotificationService.send/sendAsync, MessageDTO record (recipients/templateCode/params/channelType), MessageChannelType enum (SMS/EMAIL/IN_APP), template engine"

add_skill "eagle-payment"             "eagle-payment-starter" \
    "Use when integrating payments (Alipay/WeChat Pay) in eagle-cloud projects — PaymentGateway interface, alipayPaymentGateway/wechatPaymentGateway beans, async notification handling via PaymentNotifyEvent, signature validation"

add_skill "eagle-scheduler"           "eagle-scheduler-starter" \
    "Use when implementing distributed scheduled tasks in eagle-cloud projects — XXL-JOB integration, @XxlJob annotation, XxlJobHelper (getJobParam/getShardIndex/log), idempotency requirements"

add_skill "eagle-seata"               "eagle-seata-starter" \
    "Use when implementing distributed transactions in eagle-cloud projects — Seata @GlobalTransactional, GlobalTransactionTemplate programmatic API, TccAction interface (tryAction/confirm/cancel), TccIdempotencyHelper"

add_skill "eagle-sentinel"            "eagle-sentinel-starter" \
    "Use when implementing rate limiting/circuit breaking in eagle-cloud projects — @RateLimit(qps/threads/behavior/warmUpPeriodSec/maxQueueingTimeMs), FlowControlBehavior enum (FAST_FAIL/WARM_UP/RATE_LIMITER, NOT REJECT), Sentinel dashboard integration"

add_skill "eagle-websocket"           "eagle-websocket-starter" \
    "Use when implementing WebSocket/SSE/offline messaging in eagle-cloud projects — WebSocketSessionManager (sendToUser/broadcast), SseEmitterManager, OfflineMessageStore (store/getAndClear/count)"

echo ""
echo "==> 完成"
echo "    rules:    $(ls "$PLUGIN_DIR/rules" | wc -l | tr -d ' ')"
echo "    commands: $(ls "$PLUGIN_DIR/commands" | wc -l | tr -d ' ')"
echo "    skills:   $(ls "$PLUGIN_DIR/skills" | wc -l | tr -d ' ')"

#!/usr/bin/env bash
# verify-rules.sh — 校验 agent-plugin/rules/ 的断言与代码实况是否一致
#
# 规则是 always-on 注入的，写错会让模型稳定地生成错误代码。
# 本脚本断言规则文本里每一个"可验证的事实"，防止规则随代码漂移。
#
# 用法：在仓库根目录执行 ./agent-plugin/scripts/verify-rules.sh
# 退出码：0 = 无偏差；1 = 有偏差（输出清单）

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

RULES_DIR="agent-plugin/rules"
FE_RULES_DIR="agent-plugin/rules-frontend"
DEVIATIONS=0

red()  { printf '\033[31m%s\033[0m\n' "$1"; }
green(){ printf '\033[32m%s\033[0m\n' "$1"; }
head2(){ printf '\n\033[1m%s\033[0m\n' "$1"; }

fail() { red "  ✗ $1"; DEVIATIONS=$((DEVIATIONS + 1)); }
ok()   { green "  ✓ $1"; }

# 排除构建产物的源码搜索
src_find() { find eagle-services eagle-starter -name "$1" -not -path '*/build/*' 2>/dev/null | head -1; }

# ---------------------------------------------------------------------------
head2 "1a. 规则里 Markdown 链接指向的源文件必须存在"
# ---------------------------------------------------------------------------
# 判据精准：作者写了 [Xxx](path/to/Xxx.java) 就是在断言该文件存在。自维护，零误报。
LINKS=$(grep -rhoE '\]\((eagle-[a-z-]+/[^)]+\.java)\)' "$RULES_DIR"/*.md | sed -E 's/^\]\(//; s/\)$//' | sort -u)
if [ -z "$LINKS" ]; then
  ok "（规则中暂无源文件链接）"
else
  for p in $LINKS; do
    if [ -f "$p" ]; then ok "$(basename "$p")"; else fail "$p —— 链接指向的文件不存在"; fi
  done
fi

# ---------------------------------------------------------------------------
head2 "1b. 模型会直接调用的 Eagle 核心 API 必须存在"
# ---------------------------------------------------------------------------
# 这些是 07-checklist.md「高频陷阱」赖以成立的类；缺任何一个，规则就在教错的 API。
for c in LogMask SecurityUtils EagleUser DistributedLock CacheProtectionUtil \
         BaseEntity BaseAggregateRoot BaseEvent AsyncConfig AuditLogRecord \
         RedisRateLimiter EncryptedStringConverter DataScope \
         ContextPropagationConfig AbstractAmqpListener AbstractDlqListener \
         ExchangeNaming EagleJwtAuthenticationConverter BusinessMetrics; do
  if [ -n "$(src_find "$c.java")" ]; then ok "$c"; else fail "$c —— 规则依赖此类，但代码中不存在"; fi
done

# ---------------------------------------------------------------------------
head2 "1c. 规则声明「不存在」的 API 必须确实不存在（反向校验）"
# ---------------------------------------------------------------------------
# 若这些东西重新出现在代码里，说明能力回归了，规则必须同步更新，否则会误导。
check_absent() {
  local name="$1" pattern="$2" where="$3"
  if grep -rqE "$pattern" --include='*.java' $where 2>/dev/null; then
    fail "$name —— 规则说它不存在，但代码里已出现，规则需更新"
  else
    ok "$name 确实不存在"
  fi
}
check_absent "@Sensitive 注解"        '@interface Sensitive\b'          "eagle-starter eagle-services"
check_absent "SensitiveStrategy"      'enum SensitiveStrategy\b'        "eagle-starter eagle-services"
check_absent "@DataPermission 注解"   '@interface DataPermission\b'     "eagle-starter eagle-services"
check_absent "DataPermissionProvider" 'interface DataPermissionProvider' "eagle-starter eagle-services"
check_absent "ApiResult 包装类"       'class ApiResult\b'               "eagle-starter eagle-services"
check_absent "RestTemplate 用法"      'import org\.springframework\.web\.client\.RestTemplate' "eagle-starter eagle-services"
check_absent "Jackson 2 databind/core" 'import com\.fasterxml\.jackson\.(databind|core)\.' "eagle-starter eagle-services"

# 随 9 个 starter 移除而消失的能力：一旦重新出现，说明能力回归了，规则必须同步更新。
check_absent "TenantContextHolder"    'class TenantContextHolder\b'     "eagle-starter eagle-services"
check_absent "@TenantFilter 注解"     '@interface TenantFilter\b'       "eagle-starter eagle-services"
check_absent "AbstractRocketMqListener" 'class AbstractRocketMqListener' "eagle-starter eagle-services"
check_absent "@GlobalTransactional 用法" 'import io\.seata\.'           "eagle-starter eagle-services"
check_absent "Sentinel 用法"          'import com\.alibaba\.csp\.sentinel' "eagle-starter eagle-services"
check_absent "Nacos 注册中心"         'import com\.alibaba\.cloud\.nacos' "eagle-starter eagle-services"

# 配置键的反向校验：规则（07-checklist #10/#11/#21、AGENTS.md）明确写「不存在这些总开关」。
# 一旦有人加回来，规则就在误导使用方，必须同步更新。
# 注意：这几个键在 key_ignored() 中对校验项 3 豁免，守护责任全在这里。
check_absent_key() {
  local key="$1"
  if grep -rqF "$key" --include='*.java' --include='*.yml' --include='*.yaml' \
       eagle-starter eagle-services 2>/dev/null; then
    fail "$key —— 规则说不存在这个总开关，但代码里已出现，规则需更新"
  else
    ok "$key 确实不存在（无总开关）"
  fi
}
check_absent_key "eagle.tenant.enabled"
check_absent_key "eagle.datasource.enabled"

# ---------------------------------------------------------------------------
head2 "2. 规则提到的表名必须在 @Table 中出现"
# ---------------------------------------------------------------------------
for t in $(grep -rhoE '`(sys|auth|user|eagle)_[a-z_]+`' "$RULES_DIR"/*.md | tr -d '`' | sort -u); do
  if grep -rq "\"$t\"" --include='*.java' eagle-services eagle-starter 2>/dev/null; then
    ok "$t"
  else
    fail "$t —— 规则写了这个表名，但代码里找不到"
  fi
done

# ---------------------------------------------------------------------------
head2 "3. 规则提到的配置键必须真实存在"
# ---------------------------------------------------------------------------
# 规则中作为「反例占位符 / 框架自带键」出现，不该拿去仓库代码里找的
key_ignored() {
  case "$1" in
    # 规则教「不存在这些键」的反例（能力已随 starter 移除，由 1c 反向校验负责）
    eagle.xxx.enabled|eagle.tenant.enabled|eagle.datasource.enabled) return 0 ;;
    eagle.tenant.mode|eagle.datasource.master.url) return 0 ;;
    spring.autoconfigure.exclude|spring.factories) return 0 ;;  # 框架自带键，无字面量
    spring.cloud.consul.*) return 0 ;;                          # 注册中心键，代码里无字面量
    *) return 1 ;;
  esac
}

check_key() {
  local key="$1"
  key_ignored "$key" && { ok "$key（反例/框架键，跳过）"; return; }

  local prefix="${key%.*}" leaf="${key##*.}"
  # kebab-case → camelCase，对应 Properties 字段名。
  # 用 awk 而非 sed 的 \u —— BSD sed（macOS 默认）不支持 \u，会静默转错。
  local leaf_camel
  leaf_camel=$(printf '%s' "$leaf" | awk -F- '{printf "%s", $1; for (i = 2; i <= NF; i++) printf "%s%s", toupper(substr($i, 1, 1)), substr($i, 2)}')
  # Micrometer 指标名同样是 eagle.* 形态，但由 PREFIX + counter("xxx") 拼出，
  # 字面量搜不到 —— 去掉 eagle. 前缀后到 counter(...) 调用里找
  local metric="${key#eagle.}"

  if grep -rqF "$key" --include='*.java' --include='*.yml' eagle-services eagle-starter 2>/dev/null \
    || grep -rqF "counter(\"$metric\"" --include='*.java' eagle-starter 2>/dev/null; then
    ok "$key"
    return
  fi

  # 拆写形式 @ConditionalOnProperty(prefix=…, name=…) / @ConfigurationProperties(prefix=…)：
  # 光有 prefix 命中**不算数** —— 否则 eagle.tenant.<任意后缀> 全都假通过，
  # 正是这个漏洞让「eagle.tenant.enabled 存在」的错误断言蒙混过关。
  # 必须在命中 prefix 的那批文件里，再找到 name="leaf" 或字段 leafCamel。
  local files
  files=$(grep -rl "prefix = \"$prefix\"" --include='*.java' eagle-starter eagle-services 2>/dev/null | grep -v /build/)
  if [ -n "$files" ] && printf '%s\n' "$files" \
       | xargs grep -qE "name = \"$leaf\"|[[:space:]]${leaf_camel}[[:space:]]*[;=]" 2>/dev/null; then
    ok "$key"
    return
  fi

  fail "$key —— 规则写了这个配置键/指标名，但代码里找不到"
}
for k in $(grep -rhoE '`eagle\.[a-z0-9.-]+`|`spring\.[a-z0-9.-]+`' "$RULES_DIR"/*.md \
           | tr -d '`' | sed 's/\.$//' | sort -u); do
  check_key "$k"
done

# ---------------------------------------------------------------------------
head2 "4. 存量违例台账数字与实测一致"
# ---------------------------------------------------------------------------
cmp_num() {
  local label="$1" actual="$2" pattern="$3"
  local claimed
  claimed=$(grep -ohE "$pattern" "$RULES_DIR"/*.md | grep -oE '[0-9]+' | head -1)
  if [ -z "$claimed" ]; then
    fail "$label —— 台账里找不到该数字，模式可能已失效"
  elif [ "$claimed" = "$actual" ]; then
    ok "$label = $actual"
  else
    fail "$label —— 台账写 ${claimed}，实测 $actual"
  fi
}
cmp_num "isAuthenticated() 违例" \
  "$(grep -rc '@PreAuthorize("isAuthenticated()")' --include='*.java' eagle-services 2>/dev/null | grep -v ':0' | awk -F: '{s+=$2} END{print s+0}')" \
  '\*\*[0-9]+ 处\*\* \| 各 Controller'
cmp_num "@Value 违例" \
  "$(grep -rE '@Value\("\$\{' --include='*.java' eagle-services eagle-starter 2>/dev/null | grep -v /build/ | wc -l | tr -d ' ')" \
  '\*\*[0-9]+ 处\*\* \| `BlacklistCacheWarmer`'
cmp_num "ThreadLocal 处数" \
  "$(grep -rl 'ThreadLocal' --include='*.java' eagle-starter eagle-services 2>/dev/null | grep -v /build/ | wc -l | tr -d ' ')" \
  '本仓库有 [0-9]+ 处 `ThreadLocal`'
# 精确统计"真正声明 record"的主代码文件（注释里出现 record 字样的不算）
cmp_num "record 主代码文件数" \
  "$(grep -rlE '(public |private |^|  )record [A-Z][A-Za-z0-9]*\(' --include='*.java' eagle-services eagle-starter 2>/dev/null | grep -v /build/ | grep '/main/' | wc -l | tr -d ' ')" \
  '主代码 [0-9]+ 个文件在用'
# DTO record 化进度：非 record 的 DTO 是存量最大缺口，数字只应变小
cmp_num "非 record 的 DTO 数" \
  "$(find eagle-services -path '*interfaces/dto/*.java' -not -path '*/build/*' -not -name '*Test.java' -exec grep -L '^public record ' {} + 2>/dev/null | wc -l | tr -d ' ')" \
  '基线 [0-9]+ —— 出现任何非'

# ---------------------------------------------------------------------------
head2 "5. 规则里的 Gradle 模块路径必须在 settings.gradle 中存在"
# ---------------------------------------------------------------------------
for m in $(grep -rhoE ':eagle-[a-z-]+:eagle-[a-z-]+' "$RULES_DIR"/*.md agent-plugin/commands/*.md 2>/dev/null | sort -u); do
  if grep -q "'${m#:}'" settings.gradle; then ok "$m"; else fail "$m —— settings.gradle 中不存在"; fi
done

# ---------------------------------------------------------------------------
head2 "6. 规则文件交叉引用无断链"
# ---------------------------------------------------------------------------
for f in $(grep -rhoE '`0[0-9]-[a-z0-9-]+\.md`' "$RULES_DIR"/*.md agent-plugin/commands/*.md 2>/dev/null | tr -d '`' | sort -u); do
  if [ -f "$RULES_DIR/$f" ]; then ok "$f"; else fail "$f —— 后端规则中引用了不存在的文件"; fi
done
for f in $(grep -rhoE '`0[0-9]-[a-z0-9-]+\.md`' "$FE_RULES_DIR"/*.md 2>/dev/null | tr -d '`' | sort -u); do
  if [ -f "$FE_RULES_DIR/$f" ]; then ok "(前端) $f"; else fail "(前端) $f —— 不存在"; fi
done

# ---------------------------------------------------------------------------
printf '\n'
if [ "$DEVIATIONS" -eq 0 ]; then
  green "════ 规则与代码一致，无偏差 ════"
  exit 0
else
  red "════ 发现 $DEVIATIONS 处偏差，请更新规则或代码 ════"
  exit 1
fi

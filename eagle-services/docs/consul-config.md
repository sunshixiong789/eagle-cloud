# Consul 配置中心 + ACL（eagle-cloud 侧）

本文覆盖 **auth / system / gateway** 三服务的 KV 布局，以及**两个环境共用的 ACL 开启流程**。
配置中心的通用模型（优先级链、`${VAR}` 占位符约定、排查手法）见
`ease-mind-servers/docs/consul-config.md`，此处不重复。

## 环境

| 环境 | Consul | 应用/中间件内网 IP | profile |
|---|---|---|---|
| dev | `http://118.24.138.189:8500` | `172.27.0.8` | `dev` |
| prod | `http://139.155.104.132:8500` | `172.27.0.155` | `prod` |

## KV 布局

```
config/auth,<profile>/data              auth
config/system,<profile>/data            system
config/gateway-server,<profile>/data    gateway
```

⚠️ **gateway 的 app 名是 `gateway-server` 不是 `gateway`**（`spring.application.name`）。
KV 路径按 app 名，不是 compose 服务名。

⚠️ 本仓库**不写** `config/application,<profile>/data`。那个共享 context 归 ease-mind-servers
管 —— KV 一个 key 存一整份 YAML，不是逐键合并，两边都写会整份互相覆盖。本仓库三服务所需的键（含 `REDIS_HOST` /
`RABBITMQ_HOST`）一律写进各自服务层；服务层优先级高于 application 层，
正好也绕开了 dev 环境「本仓库走容器名 `redis`，ease-mind 走宿主 IP」的取值冲突。

## 留在 compose 的项（不进 KV）

**引导参数** —— 决定「去哪台 Consul、读哪个 profile、以什么地址注册」，放进 Consul 是鸡生蛋。
这些**不是机密**，已固化成各 compose 的默认值：
`SPRING_PROFILES_ACTIVE` `CONSUL_HOST` `CONSUL_PORT` `CONSUL_INSTANCE_IP`
`CONSUL_DISCOVERY_ENABLED` `CONSUL_CONFIG_ENABLED`

**每服务固定值** —— 属服务身份而非环境配置：
`SERVER_PORT` `DB_NAME`(auth=eagle_auth / system=eagle_system) `REDIS_DATABASE`
`JAVA_OPTS` `JAVA_TOOL_OPTIONS` `TZ`

## 部署：仓库里没有 .env 文件

`.env.dev` / `.env.prod` 已于 2026-08-07 删除（备份在 `~/.eagle-env-backup/`）。
除 token 外的引导参数都在 compose 默认值里，所以**不需要 `--env-file`**。

唯一要外部提供的是 `CONSUL_TOKEN`。它是机密，刻意**不**写进 compose —— compose 要进 git，
而 `.env` 本来就 gitignore；把 token 从 gitignore 的文件挪进被追踪的文件是安全倒退。

行内注入即可，一次性、不落盘：

```bash
CONSUL_TOKEN=<service-token> docker compose -f docker-compose.yml up -d --build
CONSUL_TOKEN=<service-token> docker compose -f docker-compose.prod.yml up -d --build
```

不想每条命令都贴 token，就在当前 shell 里读一次（`-s` 不回显，不进 history）：

```bash
read -rs CONSUL_TOKEN && export CONSUL_TOKEN
docker compose -f docker-compose.yml up -d --build
docker compose -f docker-compose.yml logs -f auth
```

不带 token 也能起，但 ACL 开着，服务读 KV 会 403、注册也会失败。
CI 场景把 `CONSUL_TOKEN` 配成流水线的 secret 变量即可。

## ACL 开启流程（每个环境各做一次）

compose 里 consul 已带 `acl.enabled=true / default_policy=deny`，**需重启容器才生效**：

```bash
docker compose -f docker-compose.yml up -d --force-recreate consul
```

然后 bootstrap。**只能成功一次**，SecretID 就是 management token，当场存好：

```bash
C=http://118.24.138.189:8500
curl -s -X PUT $C/v1/acl/bootstrap        # 取返回里的 SecretID
```

建两个 policy，再签一个 service token：

```bash
MGMT=<上一步的 SecretID>
curl -s -X PUT -H "X-Consul-Token: $MGMT" "$C/v1/acl/policy" -d '{
  "Name":"eagle-service",
  "Rules":"service_prefix \"\" { policy = \"write\" } node_prefix \"\" { policy = \"read\" } key_prefix \"config/\" { policy = \"read\" } session_prefix \"\" { policy = \"write\" } agent_prefix \"\" { policy = \"read\" }"}'

curl -s -X PUT -H "X-Consul-Token: $MGMT" "$C/v1/acl/token" -d '{
  "Description":"eagle service token","Policies":[{"Name":"eagle-service"}]}'   # 取 SecretID
```

产出两个 token：

- **management token** ≈ root，只在初始化和改 KV 时用。存密码管理器，**不要落进任何仓库文件**。
- **service token** 权限仅够「注册服务 + 只读 `config/` 前缀」，部署时作为 `CONSUL_TOKEN`
  注入，6 个服务共用。刻意不给 KV 写权限 —— 写 KV 走 management token。

还要给 agent 自己下发一个 token，否则 agent 反熵同步会持续报 ACL 错
（`enable_token_persistence=true` 使其落盘，重启不丢）：

```bash
curl -s -X PUT -H "X-Consul-Token: $MGMT" "$C/v1/agent/token/agent" \
  -d "{\"Token\":\"$MGMT\"}"
```

开 ACL 后 **Web UI 也要登录**：右上角 "Log in" 填 management token 的 SecretID
（不是 AccessorID —— AccessorID 只是标识符，不能用于认证）。

## 改配置

KV 一个 key 存**一整份 YAML**，所以改任何一项都是「读出整份 → 改 → 整份写回」。
Web UI 里直接编辑那段 YAML 是最省事的；命令行走这三步：

```bash
C=http://118.24.138.189:8500; MGMT=<management-token>; K='config/auth,dev/data'

curl -s -H "X-Consul-Token: $MGMT" "$C/v1/kv/$K?raw" > /tmp/cfg.yaml   # 读
$EDITOR /tmp/cfg.yaml                                                  # 改
curl -s -X PUT -H "X-Consul-Token: $MGMT" --data-binary @/tmp/cfg.yaml "$C/v1/kv/$K"
rm -f /tmp/cfg.yaml                                                    # 别把明文留在盘上
```

列出所有 context / 看某份现状：

```bash
curl -s -H "X-Consul-Token: $MGMT" "$C/v1/kv/config?recurse=true&keys=true"
curl -s -H "X-Consul-Token: $MGMT" "$C/v1/kv/config/auth,dev/data?raw"
```

**配置只在启动期读取一次**（`consul.config.watch.enabled=false`），改完 KV 必须重启服务。

⚠️ 整份覆盖意味着**并发编辑会互相丢改动**。多人同时动同一个 key 时用 CAS：
先读 `?raw` 的同时取 `ModifyIndex`（去掉 `?raw` 读元数据），写回时带 `?cas=<index>`，
返回 `false` 说明期间被人改过，重读再来。

## 迁移中踩到并已修掉的坑

1. **`import: optional:consul:` 是非法 YAML** —— 结尾冒号被解析成嵌套映射，服务直接起不来。
   必须写 `import: "optional:consul:"`。
2. **`DB_NAME` 键名 ≠ 取值变量名** —— compose 里是 `DB_NAME=${AUTH_DB_NAME:-eagle_auth}`，
   按键名去 .env 读会拿到 system 的 `eagle_system`，auth 连错库。现已改为 compose 内硬编码。
3. **relaxed binding 在 KV 里不生效** —— `EAGLE_OAUTH_ISSUER`→`eagle.oauth.issuer` 这种
   大写下划线映射只对系统环境变量属性源生效。yml 里没有 `${VAR}` 占位符的键写进 KV 会**静默失效**。
   本仓库唯一有实际消费方的是 `EAGLE_ENCRYPT_SECRET_KEY`，已特判成规范键 `eagle.encrypt.secret-key`。
4. **system 有 23 个死注入** —— auth 从 system 拆出后，compose 仍在给 system 注入
   SMS/WECHAT/OAUTH/JWT 配置，但 system 代码里 0 引用、也没有 notification starter。
   已不迁入 KV，避免扩大机密暴露面。
5. **`RABBITMQ_EXCHANGE_PREFIX`** —— auth/system 的 application.yml 默认值是 `dev_`，
   `application-prod.yml` 未覆盖，prod 的 `prod_` 此前完全靠 compose 注入。
   已写进 prod 的 KV，否则 prod 会用 dev_ 前缀与开发环境共用 exchange。
6. **`EAGLE_REMEMBER_ME_KEY` 无默认值且 compose 从未注入** —— `.env` 里有值，但没在
   compose 的 environment 段声明过，docker 部署下 auth 会因
   `Could not resolve placeholder` 启动失败。已纳入 KV。
7. **空值不能写进 KV** —— Spring 的 `${VAR:default}` 只在属性**不存在**时回落，
   空串会原样绑定并顶掉 yml 默认值。推送脚本一律跳过空值。

## 安全边界

ACL 解决的是「谁能读写 Consul」，**不解决「KV 里躺着明文凭据」**。Consul OSS 的 KV
没有字段级加密，raft 快照、备份、UI、任何持 read 权限的 token 拿到的都是明文。

因此以下两条**必须同时做**，不能互相替代：

- ACL（本文流程）
- 安全组把 8500/8600 收窄到仅应用机内网可达 —— 目前两个环境的 8500 都能从公网访问

彻底方案是把机密移到 Vault（`spring-cloud-vault`），Consul KV 只留非机密配置。
那是另一套基础设施，本次未做。

## 回滚

任一服务临时不走配置中心：把该服务的 `CONSUL_CONFIG_ENABLED=false`，
所有值回落到 `application.yml` 的 `${VAR:default}`（多指向 localhost，仅用于
判断「问题是不是出在 Consul」）。要恢复旧模式，把变量重新写回 compose 的
environment 段即可 —— 环境变量优先级高于 KV，会直接盖掉。

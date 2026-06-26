-- ============================================================================
-- ease-mind + eagle-cloud 开发环境数据库初始化（手动执行版）
--
-- 作用：在你现有的 PG 上把缺的库一次性建好。只增不删、可重复执行（幂等）：
--       已存在的库/角色会被跳过，绝不触碰已有数据。没有任何 DROP / TRUNCATE。
--
-- 连接：用超级用户（dev 下通常是 postgres）连到目标 PG 的【任意已存在库】
--       （postgres 或 template1 都行），整份执行即可。
--
-- 推荐 psql（下面用到 \gexec，是 psql 的元命令）：
--   psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -f init-databases-dev.sql
--
-- 用 DBeaver / Navicat / pgAdmin 等 GUI：不认 \gexec —— 见文件末尾「GUI 执行方式」。
--
-- 库与连接方的对应：
--   省心三服务（以 POSTGRES_USERNAME 连接，dev 默认 postgres）：
--     shengxin_user / shengxin_product / shengxin_trade   —— owner = 执行本脚本的当前用户
--   eagle-cloud 服务（以 DB_USERNAME 连接，默认 eagle）：
--     eagle_system / eagle_auth / eagle_payment           —— owner = eagle 角色
--   ⚠️ 只跑省心后端、不带 eagle-cloud 时：只执行 ① 即可，跳过 ② ③。
-- ============================================================================


-- ① 省心三库（owner = 当前连接用户；dev 下你用 postgres 连接，库即归 postgres，
--    与三服务 POSTGRES_USERNAME=postgres 一致）
SELECT 'CREATE DATABASE shengxin_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shengxin_user')\gexec

SELECT 'CREATE DATABASE shengxin_product'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shengxin_product')\gexec

SELECT 'CREATE DATABASE shengxin_trade'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shengxin_trade')\gexec


-- ② eagle 登录角色（eagle-cloud 服务以此连接；要改密码就改这一行的 'eagle123456'）
--    DO 块幂等：角色已存在则跳过，不会重置已有角色的密码。
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'eagle') THEN
    CREATE ROLE eagle LOGIN PASSWORD 'eagle123456';
  END IF;
END
$$;


-- ③ eagle 三库（owner = eagle，owner 天然拥有全部权限，无需再 GRANT）
SELECT 'CREATE DATABASE eagle_system OWNER eagle'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eagle_system')\gexec

SELECT 'CREATE DATABASE eagle_auth OWNER eagle'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eagle_auth')\gexec

SELECT 'CREATE DATABASE eagle_payment OWNER eagle'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eagle_payment')\gexec


-- ============================================================================
-- GUI 执行方式（DBeaver / Navicat / pgAdmin 等不支持 \gexec）
--
--   ② 的 DO 块 GUI 能直接跑，照常执行即可。
--   ① ③ 的建库改成下面的「裸 CREATE DATABASE」逐条执行：
--     库不存在 → 建好；库已存在 → 报 "database already exists"，无害，忽略即可
--     （CREATE DATABASE 永远不会删库，不存在数据丢失风险）。
--
--   CREATE DATABASE shengxin_user;
--   CREATE DATABASE shengxin_product;
--   CREATE DATABASE shengxin_trade;
--   CREATE DATABASE eagle_system  OWNER eagle;
--   CREATE DATABASE eagle_auth    OWNER eagle;
--   CREATE DATABASE eagle_payment OWNER eagle;
-- ============================================================================

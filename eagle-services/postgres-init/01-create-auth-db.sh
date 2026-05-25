#!/bin/bash
# Postgres 容器首次初始化时执行（数据卷为空时才会触发）。
# 在 POSTGRES_DB 之外再建 eagle_auth 库，给 auth-service 单独使用。
# 若需要重置：先 `docker compose down -v` 删除 postgres-data 卷再启动。
set -e

AUTH_DB="${AUTH_DB_NAME:-eagle_auth}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE ${AUTH_DB}'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${AUTH_DB}')\gexec
    GRANT ALL PRIVILEGES ON DATABASE ${AUTH_DB} TO ${POSTGRES_USER};
EOSQL

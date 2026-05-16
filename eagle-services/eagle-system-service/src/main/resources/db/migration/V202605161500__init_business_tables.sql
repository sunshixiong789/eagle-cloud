-- =====================================================================
-- V202605161500: 业务实体表 baseline（auth 域 + base 域）
--
-- 用 Hibernate schema-generation 导出后整理；适用于 H2 / MySQL 8。
-- 后续业务表变更通过新增 V{yyyyMMddHHmm}__*.sql 文件（28-migration.md）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- auth_account（Account 聚合根）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version     BIGINT                                COMMENT '乐观锁版本号',
    create_by   BIGINT                                COMMENT '创建人 ID',
    create_time TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by   BIGINT                                COMMENT '更新人 ID',
    update_time TIMESTAMP                             COMMENT '更新时间',
    username    VARCHAR(64)  NOT NULL                 COMMENT '用户名',
    password    VARCHAR(128) NOT NULL                 COMMENT '密码（BCrypt）',
    phone       VARCHAR(20)                           COMMENT '手机号',
    locked      BOOLEAN      NOT NULL                 COMMENT '是否锁定',
    openid      VARCHAR(128)                          COMMENT '微信小程序 openid',
    unionid     VARCHAR(128)                          COMMENT '微信 unionid',
    web_openid  VARCHAR(128)                          COMMENT '微信网页（PC 扫码）openid',
    mp_openid   VARCHAR(128)                          COMMENT '微信公众号网页授权 openid',
    bind_time   TIMESTAMP                             COMMENT '微信绑定时间',
    PRIMARY KEY (id),
    CONSTRAINT idx_account_username UNIQUE (username)
);
CREATE INDEX IF NOT EXISTS idx_account_phone      ON auth_account (phone);
CREATE INDEX IF NOT EXISTS idx_account_openid     ON auth_account (openid);
CREATE INDEX IF NOT EXISTS idx_account_unionid    ON auth_account (unionid);
CREATE INDEX IF NOT EXISTS idx_account_web_openid ON auth_account (web_openid);
CREATE INDEX IF NOT EXISTS idx_account_mp_openid  ON auth_account (mp_openid);

-- ---------------------------------------------------------------------
-- oauth2_client（OAuthClient 聚合根）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oauth2_client (
    id                              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version                         BIGINT                                COMMENT '乐观锁版本号',
    create_by                       BIGINT                                COMMENT '创建人 ID',
    create_time                     TIMESTAMP     NOT NULL                COMMENT '创建时间',
    update_by                       BIGINT                                COMMENT '更新人 ID',
    update_time                     TIMESTAMP                             COMMENT '更新时间',
    client_id                       VARCHAR(100)  NOT NULL                COMMENT '客户端 ID',
    client_name                     VARCHAR(200)  NOT NULL                COMMENT '客户端名称',
    client_secret                   VARCHAR(200)                          COMMENT '客户端密钥（BCrypt 编码）',
    client_authentication_methods   VARCHAR(1000) NOT NULL                COMMENT '认证方式，逗号分隔',
    authorization_grant_types       VARCHAR(1000) NOT NULL                COMMENT '授权类型，逗号分隔',
    redirect_uris                   VARCHAR(2000)                         COMMENT '重定向 URI，逗号分隔',
    scopes                          VARCHAR(1000) NOT NULL                COMMENT '授权范围，逗号分隔',
    require_proof_key               BOOLEAN                               COMMENT '是否要求 PKCE',
    require_authorization_consent   BOOLEAN                               COMMENT '是否要求授权同意',
    access_token_ttl_seconds        BIGINT        NOT NULL                COMMENT 'Access Token 有效期（秒）',
    refresh_token_ttl_seconds       BIGINT        NOT NULL                COMMENT 'Refresh Token 有效期（秒）',
    client_id_issued_at             TIMESTAMP                             COMMENT '客户端 ID 签发时间',
    enabled                         BOOLEAN       NOT NULL                COMMENT '是否启用',
    PRIMARY KEY (id),
    CONSTRAINT idx_client_id UNIQUE (client_id)
);

-- ---------------------------------------------------------------------
-- sys_role（Role 聚合根）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version     BIGINT                                COMMENT '乐观锁版本号',
    create_by   BIGINT                                COMMENT '创建人 ID',
    create_time TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by   BIGINT                                COMMENT '更新人 ID',
    update_time TIMESTAMP                             COMMENT '更新时间',
    role_name   VARCHAR(64)  NOT NULL                 COMMENT '角色名称',
    role_code   VARCHAR(64)  NOT NULL                 COMMENT '角色标识',
    role_desc   VARCHAR(255)                          COMMENT '角色描述',
    role_type   VARCHAR(20)  NOT NULL                 COMMENT '角色类型（BUSINESS/SYSTEM）',
    data_scope  VARCHAR(20)  NOT NULL                 COMMENT '数据范围（ALL/CUSTOM/DEPT/DEPT_AND_CHILD/SELF）',
    sort_order  INT          NOT NULL                 COMMENT '排序值',
    status      VARCHAR(20)  NOT NULL                 COMMENT '角色状态（NORMAL/DISABLED/DELETED）',
    PRIMARY KEY (id),
    CONSTRAINT idx_role_code UNIQUE (role_code)
);
CREATE INDEX IF NOT EXISTS idx_role_type   ON sys_role (role_type);
CREATE INDEX IF NOT EXISTS idx_status_role ON sys_role (status);

-- ---------------------------------------------------------------------
-- sys_dict（Dict 聚合根）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version     BIGINT                                COMMENT '乐观锁版本号',
    create_by   BIGINT                                COMMENT '创建人 ID',
    create_time TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by   BIGINT                                COMMENT '更新人 ID',
    update_time TIMESTAMP                             COMMENT '更新时间',
    dict_type   VARCHAR(50)  NOT NULL                 COMMENT '字典类型',
    dict_name   VARCHAR(100) NOT NULL                 COMMENT '字典名称',
    description VARCHAR(500)                          COMMENT '字典描述',
    system_flag BOOLEAN      NOT NULL                 COMMENT '是否系统内置',
    status      VARCHAR(20)  NOT NULL                 COMMENT '字典状态（ACTIVE/INACTIVE/DELETED）',
    remarks     VARCHAR(500)                          COMMENT '备注',
    PRIMARY KEY (id),
    CONSTRAINT idx_dict_type UNIQUE (dict_type)
);

-- ---------------------------------------------------------------------
-- sys_dict_item（DictItemEntity 子实体）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version     BIGINT                                COMMENT '乐观锁版本号',
    create_by   BIGINT                                COMMENT '创建人 ID',
    create_time TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by   BIGINT                                COMMENT '更新人 ID',
    update_time TIMESTAMP                             COMMENT '更新时间',
    dict_id     BIGINT       NOT NULL                 COMMENT '所属字典 ID',
    item_value  VARCHAR(100) NOT NULL                 COMMENT '字典项值',
    name        VARCHAR(100) NOT NULL                 COMMENT '字典项标签',
    dict_type   VARCHAR(50)  NOT NULL                 COMMENT '字典类型（冗余）',
    parent_id   BIGINT       NOT NULL                 COMMENT '父级字典项 ID，0 表示顶级',
    description VARCHAR(500)                          COMMENT '描述',
    sort_order  INT                                   COMMENT '排序值',
    status      VARCHAR(20)  NOT NULL                 COMMENT '字典项状态',
    remarks     VARCHAR(500)                          COMMENT '备注',
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dict_item_dict_id   ON sys_dict_item (dict_id);
CREATE INDEX IF NOT EXISTS idx_dict_item_dict_type ON sys_dict_item (dict_type);
CREATE INDEX IF NOT EXISTS idx_dict_item_value     ON sys_dict_item (item_value);
CREATE INDEX IF NOT EXISTS idx_dict_item_parent_id ON sys_dict_item (parent_id);

-- ---------------------------------------------------------------------
-- sys_log（SysLog 实体）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version     BIGINT                                COMMENT '乐观锁版本号',
    create_by   BIGINT                                COMMENT '创建人 ID',
    create_time TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by   BIGINT                                COMMENT '更新人 ID',
    update_time TIMESTAMP                             COMMENT '更新时间',
    log_type    VARCHAR(20)  NOT NULL                 COMMENT '日志类型（LOGIN/LOGOUT/API_CALL/OPERATION/EXCEPTION）',
    title       VARCHAR(255) NOT NULL                 COMMENT '日志标题',
    user_id     BIGINT                                COMMENT '用户 ID',
    username    VARCHAR(64)                           COMMENT '用户名',
    remote_addr VARCHAR(50)                           COMMENT '请求 IP 地址',
    user_agent  VARCHAR(500)                          COMMENT '用户代理',
    request_uri VARCHAR(500)                          COMMENT '请求 URI',
    method      VARCHAR(10)                           COMMENT '请求方法',
    params      TEXT                                  COMMENT '请求参数',
    result      TEXT                                  COMMENT '响应结果',
    time        BIGINT                                COMMENT '执行时间（毫秒）',
    exception   TEXT                                  COMMENT '异常信息',
    service_id  VARCHAR(64)                           COMMENT '服务 ID',
    status      VARCHAR(20)  NOT NULL                 COMMENT '日志状态（SUCCESS/FAILURE/PARTIAL_SUCCESS）',
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_log_type     ON sys_log (log_type);
CREATE INDEX IF NOT EXISTS idx_create_time  ON sys_log (create_time);
CREATE INDEX IF NOT EXISTS idx_user_id      ON sys_log (user_id);

-- ---------------------------------------------------------------------
-- sys_user（User 聚合根，含 UserProfile/Address 值对象嵌入字段）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    version         BIGINT                                COMMENT '乐观锁版本号',
    create_by       BIGINT                                COMMENT '创建人 ID',
    create_time     TIMESTAMP    NOT NULL                 COMMENT '创建时间',
    update_by       BIGINT                                COMMENT '更新人 ID',
    update_time     TIMESTAMP                             COMMENT '更新时间',
    account_id      BIGINT       NOT NULL                 COMMENT '关联的认证账号 ID',
    username        VARCHAR(64)  NOT NULL                 COMMENT '用户名（冗余，来源于 Account）',
    email           VARCHAR(100)                          COMMENT '邮箱',
    dept_id         BIGINT                                COMMENT '部门 ID',
    -- UserProfile 嵌入字段
    avatar          VARCHAR(500)                          COMMENT '头像 URL',
    nickname        VARCHAR(64)                           COMMENT '昵称',
    name            VARCHAR(64)                           COMMENT '真实姓名',
    gender          VARCHAR(20)                           COMMENT '性别（FEMALE/MALE/UNKNOWN）',
    bio             VARCHAR(255)                          COMMENT '个人简介',
    -- Address 嵌入字段
    country         VARCHAR(100)                          COMMENT '国家',
    state           VARCHAR(100)                          COMMENT '省',
    city            VARCHAR(100)                          COMMENT '市',
    detail_address  VARCHAR(255)                          COMMENT '详细地址',
    street          VARCHAR(500)                          COMMENT '街道',
    zip_code        VARCHAR(20)                           COMMENT '邮编',
    PRIMARY KEY (id),
    CONSTRAINT idx_account_id UNIQUE (account_id)
);
CREATE INDEX IF NOT EXISTS idx_username    ON sys_user (username);
CREATE INDEX IF NOT EXISTS idx_email       ON sys_user (email);
CREATE INDEX IF NOT EXISTS idx_user_dept_id ON sys_user (dept_id);

-- ---------------------------------------------------------------------
-- sys_user_role（User.roleIds @ElementCollection）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT          COMMENT '角色 ID'
);
CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON sys_user_role (user_id);

-- ---------------------------------------------------------------------
-- sys_user_post（User.postIds @ElementCollection）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_post (
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    post_id BIGINT          COMMENT '岗位 ID'
);
CREATE INDEX IF NOT EXISTS idx_user_post_user_id ON sys_user_post (user_id);

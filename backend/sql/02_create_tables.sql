-- =============================================
-- 表结构创建脚本
-- =============================================

-- 1. 企业表
CREATE TABLE sys_enterprise (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(100) UNIQUE,
    contact_name    VARCHAR(100),
    contact_email   VARCHAR(200),
    contact_phone   VARCHAR(20),
    max_users       INTEGER DEFAULT 10,
    max_knowledge_bases INTEGER DEFAULT 5,
    max_agents      INTEGER DEFAULT 5,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expire_at       TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_enterprise_tenant ON sys_enterprise(tenant_id);
CREATE INDEX idx_enterprise_code ON sys_enterprise(code) WHERE deleted = FALSE;

-- 2. 用户表
CREATE TABLE sys_user (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    role            VARCHAR(50) NOT NULL DEFAULT 'USER',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(50),
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (username, deleted),
    UNIQUE (email, deleted)
);

CREATE INDEX idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_user_username ON sys_user(username) WHERE deleted = FALSE;
CREATE INDEX idx_user_email ON sys_user(email) WHERE deleted = FALSE;
CREATE INDEX idx_user_role ON sys_user(role) WHERE deleted = FALSE;

-- 3. 角色权限表
CREATE TABLE sys_role (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (code, deleted)
);

CREATE INDEX idx_role_tenant ON sys_role(tenant_id);

-- 4. 用户角色关联表
CREATE TABLE sys_user_role (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    user_id         BIGINT NOT NULL REFERENCES sys_user(id),
    role_id         BIGINT NOT NULL REFERENCES sys_role(id),
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (user_id, role_id, deleted)
);

CREATE INDEX idx_user_role_user ON sys_user_role(user_id);
CREATE INDEX idx_user_role_role ON sys_user_role(role_id);

-- 5. 权限表
CREATE TABLE sys_permission (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(200) NOT NULL,
    type            VARCHAR(50) NOT NULL DEFAULT 'API',
    resource        VARCHAR(200),
    action          VARCHAR(50),
    description     VARCHAR(500),
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (code, deleted)
);

CREATE INDEX idx_permission_tenant ON sys_permission(tenant_id);

-- 6. 角色权限关联表
CREATE TABLE sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    role_id         BIGINT NOT NULL REFERENCES sys_role(id),
    permission_id   BIGINT NOT NULL REFERENCES sys_permission(id),
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (role_id, permission_id, deleted)
);

CREATE INDEX idx_role_perm_role ON sys_role_permission(role_id);
CREATE INDEX idx_role_perm_perm ON sys_role_permission(permission_id);

-- 7. 知识库表
CREATE TABLE knowledge_base (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(1000),
    owner_id            BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    document_count      INTEGER DEFAULT 0,
    total_chunks        INTEGER DEFAULT 0,
    embedding_model     VARCHAR(100) DEFAULT 'text-embedding-ada-002',
    vector_db_type      VARCHAR(50) DEFAULT 'chroma',
    vector_collection   VARCHAR(200),
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_kb_tenant ON knowledge_base(tenant_id);
CREATE INDEX idx_kb_owner ON knowledge_base(owner_id);
CREATE INDEX idx_kb_status ON knowledge_base(status) WHERE deleted = FALSE;

-- 8. 知识库文档表
CREATE TABLE knowledge_document (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    knowledge_base_id   BIGINT NOT NULL REFERENCES knowledge_base(id),
    title               VARCHAR(500) NOT NULL,
    file_name           VARCHAR(500),
    file_type           VARCHAR(50),
    file_size           BIGINT,
    file_path           VARCHAR(1000),
    content             TEXT,
    chunk_count         INTEGER DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    chunk_strategy      VARCHAR(50) DEFAULT 'PARAGRAPH',
    chunk_size          INTEGER DEFAULT 500,
    chunk_overlap       INTEGER DEFAULT 50,
    error_message       VARCHAR(2000),
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_kd_tenant ON knowledge_document(tenant_id);
CREATE INDEX idx_kd_kb ON knowledge_document(knowledge_base_id);
CREATE INDEX idx_kd_status ON knowledge_document(status) WHERE deleted = FALSE;

-- 9. 文档分块表
CREATE TABLE document_chunk (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    document_id         BIGINT NOT NULL REFERENCES knowledge_document(id),
    knowledge_base_id   BIGINT NOT NULL,
    chunk_index         INTEGER NOT NULL,
    content             TEXT NOT NULL,
    content_hash        VARCHAR(64),
    word_count          INTEGER,
    char_count          INTEGER,
    vector_id           VARCHAR(200),
    is_vectorized       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_dc_tenant ON document_chunk(tenant_id);
CREATE INDEX idx_dc_document ON document_chunk(document_id);
CREATE INDEX idx_dc_kb ON document_chunk(knowledge_base_id);
CREATE INDEX idx_dc_vector ON document_chunk(vector_id) WHERE vector_id IS NOT NULL;
CREATE INDEX idx_dc_vectorized ON document_chunk(knowledge_base_id, is_vectorized) WHERE deleted = FALSE;

-- 10. AI智能体表
CREATE TABLE ai_agent (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(1000),
    owner_id            BIGINT NOT NULL,
    model_provider      VARCHAR(50) NOT NULL,
    model_name          VARCHAR(100) NOT NULL,
    system_prompt       TEXT,
    temperature         DOUBLE PRECISION DEFAULT 0.7,
    max_tokens          INTEGER DEFAULT 2000,
    top_p               DOUBLE PRECISION DEFAULT 1.0,
    frequency_penalty   DOUBLE PRECISION DEFAULT 0.0,
    presence_penalty    DOUBLE PRECISION DEFAULT 0.0,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    knowledge_base_id   BIGINT REFERENCES knowledge_base(id),
    scenario            VARCHAR(100),
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_agent_tenant ON ai_agent(tenant_id);
CREATE INDEX idx_agent_owner ON ai_agent(owner_id);
CREATE INDEX idx_agent_scenario ON ai_agent(scenario) WHERE deleted = FALSE;
CREATE INDEX idx_agent_kb ON ai_agent(knowledge_base_id) WHERE knowledge_base_id IS NOT NULL;

-- 11. 智能体Prompt模板表
CREATE TABLE prompt_template (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    agent_id            BIGINT NOT NULL REFERENCES ai_agent(id),
    name                VARCHAR(200) NOT NULL,
    template_content    TEXT NOT NULL,
    variables           VARCHAR(2000),
    version             INTEGER NOT NULL DEFAULT 1,
    is_default          BOOLEAN NOT NULL DEFAULT FALSE,
    description         VARCHAR(1000),
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_pt_tenant ON prompt_template(tenant_id);
CREATE INDEX idx_pt_agent ON prompt_template(agent_id);
CREATE INDEX idx_pt_default ON prompt_template(agent_id, is_default) WHERE deleted = FALSE;

-- 12. 用户风格画像表
CREATE TABLE user_style_profile (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL DEFAULT 0,
    user_id                 BIGINT NOT NULL UNIQUE,
    formality_level         DOUBLE PRECISION DEFAULT 0.5,
    verbosity_level         DOUBLE PRECISION DEFAULT 0.5,
    emoji_frequency         DOUBLE PRECISION DEFAULT 0.3,
    preferred_tone          VARCHAR(50) DEFAULT 'neutral',
    common_phrases          TEXT,
    vocabulary_level        VARCHAR(50) DEFAULT 'standard',
    avg_sentence_length     DOUBLE PRECISION DEFAULT 20.0,
    preferred_language_mix  VARCHAR(100) DEFAULT 'zh',
    style_keywords          TEXT,
    sample_count            INTEGER DEFAULT 0,
    last_analyzed_at        TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_usp_user ON user_style_profile(user_id) WHERE deleted = FALSE;

-- 13. 用户行为记录表
CREATE TABLE user_behavior_record (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL,
    user_id                 BIGINT NOT NULL,
    session_id              VARCHAR(200),
    action_type             VARCHAR(50) NOT NULL,
    original_text           TEXT,
    modified_text           TEXT,
    context                 VARCHAR(500),
    app_package             VARCHAR(200),
    scene                   VARCHAR(100),
    accepted_suggestion     VARCHAR(500),
    rejected_suggestions     TEXT,
    extra_data              TEXT,
    created_by              BIGINT,
    updated_by              BIGINT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ubr_tenant_user ON user_behavior_record(tenant_id, user_id);
CREATE INDEX idx_ubr_action ON user_behavior_record(action_type) WHERE deleted = FALSE;
CREATE INDEX idx_ubr_created ON user_behavior_record(created_at);
CREATE INDEX idx_ubr_scene ON user_behavior_record(scene) WHERE deleted = FALSE;

-- 14. 用户知识库修正表
CREATE TABLE user_knowledge_correction (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    knowledge_base_id   BIGINT NOT NULL,
    document_id         BIGINT,
    chunk_id            BIGINT,
    original_content    TEXT,
    corrected_content   TEXT NOT NULL,
    correction_type     VARCHAR(50),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by         BIGINT,
    review_comment      VARCHAR(1000),
    reviewed_at         TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ukc_tenant ON user_knowledge_correction(tenant_id);
CREATE INDEX idx_ukc_user ON user_knowledge_correction(user_id);
CREATE INDEX idx_ukc_kb ON user_knowledge_correction(knowledge_base_id);
CREATE INDEX idx_ukc_status ON user_knowledge_correction(status) WHERE deleted = FALSE;

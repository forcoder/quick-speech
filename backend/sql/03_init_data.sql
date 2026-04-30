-- =============================================
-- 初始数据脚本
-- =============================================

-- 初始化角色
INSERT INTO sys_role (tenant_id, name, code, description, status) VALUES
(0, '超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', 'ACTIVE'),
(0, '企业管理员', 'ENTERPRISE_ADMIN', '企业管理员，管理企业内所有资源', 'ACTIVE'),
(0, '普通用户', 'USER', '普通用户，使用基本功能', 'ACTIVE'),
(0, '访客', 'GUEST', '访客，仅有查看权限', 'ACTIVE');

-- 初始化权限
INSERT INTO sys_permission (tenant_id, name, code, type, resource, action, description) VALUES
-- 用户管理
(0, '用户查看', 'user:read', 'API', 'user', 'read', '查看用户信息'),
(0, '用户创建', 'user:create', 'API', 'user', 'create', '创建用户'),
(0, '用户更新', 'user:update', 'API', 'user', 'update', '更新用户信息'),
(0, '用户删除', 'user:delete', 'API', 'user', 'delete', '删除用户'),
-- 知识库管理
(0, '知识库查看', 'knowledge:read', 'API', 'knowledge', 'read', '查看知识库'),
(0, '知识库创建', 'knowledge:create', 'API', 'knowledge', 'create', '创建知识库'),
(0, '知识库更新', 'knowledge:update', 'API', 'knowledge', 'update', '更新知识库'),
(0, '知识库删除', 'knowledge:delete', 'API', 'knowledge', 'delete', '删除知识库'),
-- 文档管理
(0, '文档查看', 'document:read', 'API', 'document', 'read', '查看文档'),
(0, '文档上传', 'document:create', 'API', 'document', 'create', '上传文档'),
(0, '文档更新', 'document:update', 'API', 'document', 'update', '更新文档'),
(0, '文档删除', 'document:delete', 'API', 'document', 'delete', '删除文档'),
-- 智能体管理
(0, '智能体查看', 'agent:read', 'API', 'agent', 'read', '查看智能体'),
(0, '智能体创建', 'agent:create', 'API', 'agent', 'create', '创建智能体'),
(0, '智能体更新', 'agent:update', 'API', 'agent', 'update', '更新智能体'),
(0, '智能体删除', 'agent:delete', 'API', 'agent', 'delete', '删除智能体'),
-- 风格管理
(0, '风格查看', 'style:read', 'API', 'style', 'read', '查看风格画像'),
(0, '风格更新', 'style:update', 'API', 'style', 'update', '更新风格画像'),
-- 行为管理
(0, '行为查看', 'behavior:read', 'API', 'behavior', 'read', '查看行为记录'),
(0, '行为创建', 'behavior:create', 'API', 'behavior', 'create', '创建行为记录'),
-- 修正管理
(0, '修正查看', 'correction:read', 'API', 'correction', 'read', '查看修正'),
(0, '修正创建', 'correction:create', 'API', 'correction', 'create', '创建修正'),
(0, '修正审核', 'correction:review', 'API', 'correction', 'review', '审核修正');

-- 创建默认企业
INSERT INTO sys_enterprise (tenant_id, name, code, contact_name, contact_email, max_users, max_knowledge_bases, max_agents, status) VALUES
(1, '默认企业', 'DEFAULT', '管理员', 'admin@quickspeech.com', 100, 50, 50, 'ACTIVE');

-- 创建默认管理员用户 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (tenant_id, username, email, password_hash, nickname, role, status) VALUES
(1, 'admin', 'admin@quickspeech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ENTERPRISE_ADMIN', 'ACTIVE');

-- 创建更新触发器
DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN
        SELECT table_name FROM information_schema.columns
        WHERE column_name = 'updated_at' AND table_schema = 'public'
    LOOP
        EXECUTE format('CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END;
$$;

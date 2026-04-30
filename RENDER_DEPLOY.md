# Render 部署指南

## 前置条件
- GitHub 仓库已推送最新代码（已完成 ✅）
- Render 账号（免费注册：https://render.com）

## 部署步骤

### 方式一：通过 Blueprint（推荐）

1. 打开 https://dashboard.render.com/blueprints/new
2. 连接 GitHub 账号（如果还没连接）
3. 选择仓库 `forcoder/quick-speech`
4. Render 会自动识别根目录的 `render.yaml`
5. 点击 **Apply**，将自动创建：
   - **Web Service**: `quick-speech-api`（Docker 容器）
   - **PostgreSQL**: `quick-speech-db`（免费数据库）

### 方式二：手动创建

1. 打开 https://dashboard.render.com/new/web
2. 选择 **Build and deploy from a Git repository**
3. 连接 `forcoder/quick-speech`
4. 配置：
   - **Name**: `quick-speech-api`
   - **Runtime**: Docker
   - **Root Directory**: `backend`
   - **Plan**: Free
5. 添加环境变量：
   - `PORT`: `8080`
   - `JWT_SECRET`: 随机生成
   - `CHROMA_URL`: 留空
   - `OPENAI_API_KEY`: 留空（可选，用于AI功能）
   - `AI_DEFAULT_MODEL`: `gpt-4o-mini`
6. 创建免费的 PostgreSQL 数据库
7. 将数据库连接信息填入环境变量

## 部署后

1. 等待构建完成（首次约 5-10 分钟）
2. 获得访问地址：`https://quick-speech-api.onrender.com`
3. 更新前端 `.env.production` 中的 API 地址
4. 重新部署前端到 GitHub Pages

## 注意事项

- 免费计划数据库有效期 **90 天**，到期需重新创建
- 免费 Web 服务 **15 分钟无请求会休眠**，首次访问有约 30 秒冷启动时间
- 需要 AI 功能时，需设置 `OPENAI_API_KEY`

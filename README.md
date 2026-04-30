# Quick Speech — 智能输入法

[![Android Build](https://github.com/quick-speech/quick-speech/actions/workflows/android-build.yml/badge.svg)](https://github.com/quick-speech/quick-speech/actions/workflows/android-build.yml)
[![Backend Build](https://github.com/quick-speech/quick-speech/actions/workflows/backend-build.yml/badge.svg)](https://github.com/quick-speech/quick-speech/actions/workflows/backend-build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

面向企业/办公人群的智能五笔输入法，以 **AI智能回复** 为核心卖点。

## ✨ 核心功能

- 📝 **五笔输入** — 支持86/98五笔方案，智能纠错，云词库同步
- 🤖 **AI智能回复** — 基于知识库和AI智能体生成建议回复
- 🔀 **混合模式** — 知识库检索 + AI智能体双引擎，综合排序最优结果
- 📚 **知识库管理** — 企业知识库上传、检索、编辑
- 🧠 **自进化学习** — 学习用户回复风格，越用越懂你
- 🎨 **简洁美观** — Material Design 3 设计规范

## 🏗️ 项目结构

```
quick-speech/
├── android/          # 安卓客户端 (Kotlin + Jetpack Compose)
│   ├── app/          # 主应用模块
│   ├── inputmethod/  # 输入法IME模块
│   ├── wubi/         # 五笔引擎模块
│   └── common/       # 公共组件
├── backend/          # 后端服务 (Spring Boot)
│   ├── api/          # REST API服务
│   ├── knowledge/    # 知识库服务
│   └── agent/        # AI智能体服务
├── admin-web/        # Web管理后台 (React + TypeScript)
├── .github/          # GitHub Actions CI/CD
└── docs/             # 设计文档
```

## 🚀 快速开始

### 安卓客户端

```bash
cd android
./gradlew assembleDebug
```

### 后端服务

```bash
cd backend
./mvnw spring-boot:run
```

### Web管理后台

```bash
cd admin-web
npm install
npm run dev
```

## 📖 设计文档

详见 [docs/superpowers/specs/2026-04-30-smart-input-method-design.md](docs/superpowers/specs/2026-04-30-smart-input-method-design.md)

## 📄 License

[MIT](LICENSE)

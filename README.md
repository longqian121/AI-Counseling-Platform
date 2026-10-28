# AI Counseling Platform

基于 Spring Boot、Spring AI 和 Vue 3 的心理支持平台。

当前功能包括流式 AI 对话、情绪日记、知识文章、医生预约、咨询总结、风险告警、访前简报和每日质检。

## 本地运行

- Java 17 或以上、Node.js 20.19+ / 22.12+、MySQL。
- 在运行环境中设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`AI_API_KEY`、`AI_BASE_URL` 和 `JWT_SECRET`。
- `DB_URL` 默认连接本地 `ai_springboot` 数据库；`AI_BASE_URL` 默认使用 `https://api.deepseek.com`。请按实际服务修改环境变量。
- 创建数据库并准备业务表。当前 SQL 文件主要位于 `src/main/resources/schema.sql` 和 `docs/feature-design/doctor-portal/migration.sql`；历史项目的数据库初始化仍需对照实体检查完整性。
- 后端：`./mvnw spring-boot:run`（Windows 使用 `mvnw.cmd spring-boot:run`），默认端口 `1236`。
- 前端：在 `ai-vue` 目录运行 `npm ci`、`npm run dev`。

不要将真实密钥、数据库密码、个人咨询记录或运行日志提交到仓库。JWT 密钥应使用独立的高强度随机值。

## 关于本次导入

本仓库以脱敏后的当前代码建立独立首次提交，不包含原仓库的 Git 历史、构建日志或运行时上传文件。

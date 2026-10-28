## 模块0：Bug修复 · 测试文档

### TC-0.1 sessionId 前缀 — 已确认正常，跳过

实际代码已统一使用 session_ 下划线。验证通过，无需修复。

### TC-0.2 UserController import 清理验证

| 项目 | 内容 |
|------|------|
| 步骤 | 删除 import lombok.Builder;，mvn compile |
| 预期 | 编译通过 |

### TC-0.3 ConsultationSession 修复验证

| 项目 | 内容 |
|------|------|
| 步骤 | 删除无用 import，补全 @NoArgsConstructor @AllArgsConstructor，mvn compile |
| 预期 | 编译通过，咨询会话功能正常 |

### TC-0.4 createSession() 异常处理验证

| 项目 | 内容 |
|------|------|
| 前置条件 | user 表无 id=99999 记录 |
| 步骤 | 用不存在的 userId 调用 createSession |
| 预期 | 抛出 BusinessException，code=6002，msg="用户不存在" |

### TC-0.5 PesponseUtil 重命名验证

| 项目 | 内容 |
|------|------|
| 步骤 | 重命名类文件，修改 JwtAuthticationFilter 中引用，mvn compile |
| 预期 | 编译通过，全局搜索 PesponseUtil 无结果 |

### TC-0.6 安全白名单扩展验证

| 项目 | 内容 |
|------|------|
| 前置条件 | 未登录 |
| 步骤 | GET /api/knowledge/article/page、GET /api/knowledge/article/xxx、GET /uploads/test.jpg |
| 预期 | 均返回 200（非 401） |

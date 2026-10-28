## 模块0：Bug修复 · 技术文档

> **状态检查日期**：2026-07-18 — 对照实际代码重新验证

### 0.1 [已修复] sessionId 前缀不一致 ✅

**实际代码确认**：PsychologicalSupportService.java 中 startSession() 和 extractSessionId() 均已使用 session_（下划线），前后一致。此 Bug 不存在，无需修复。

### 0.2 [仍存在] UserController @Builder 导致 DI 风险

**文件**：controller/User.java

`java
import lombok.Builder;  // ← 需删除
// ...
@RestController
@RequestMapping("/api/user")
public class User {  // @Builder 未在此类上标注但 import 存在
`

实际代码中 Controller 类上**没有** @Builder 注解，但有 import lombok.Builder。这个 import 无实际危害但属冗余，一并清理。

### 0.3 [仍存在] ConsultationSession 错误 import + 缺少构造器注解

**文件**：entity/ConsultationSession.java

1. 删除 import org.springframework.data.relational.core.mapping.Table;
2. **新增发现**：该 entity 只有 @Data + @Builder，缺少 @NoArgsConstructor + @AllArgsConstructor。对比 User.java entity 有四个注解齐备。缺少无参构造可能在特定 MyBatis-Plus 场景下导致实例化失败。**建议补全**。

### 0.4 [仍存在] createSession() 返回 null

**文件**：service/ConsultationSessionService.java

ResultCode 已有 USER_NOT_EXIST("6002", "用户不存在")，无需新增错误码。

### 0.5 [仍存在] Tomcat import + PesponseUtil 拼写

**文件**：util/JwtAuthticationFilter.java、util/PesponseUtil.java

### 0.6 [仍存在] SecurityConfig 白名单太窄

当前 PUBLIC_PATHS 仅 4 个路径。需追加：/api/knowledge/article/page、/api/knowledge/article/*、/uploads/**。

### 0.7 [新增发现] Token header 命名不一致（注意项，不阻塞）

- JwtTokenUtil.extractTokenFromRequest() 读 equest.getHeader("token")（小写）
- 前端 consultation.vue 的 etchEventSource 用 'Token'（大写）
- axios 拦截器发的是 Authorization header

HTTP header 名称按规范大小写不敏感，且 Controller 中 getCurrentToken() 优先从 Filter 缓存的 equest.getAttribute("jwtToken") 取值，当前不影响功能。统一为 Authorization 更规范，可后续优化。

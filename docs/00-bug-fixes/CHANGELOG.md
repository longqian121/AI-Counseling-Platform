# 模块0 Bug 修复 · 变更记录

> 修复日期：2026-07-18
> 编译结果：BUILD SUCCESS (35 source files)

## TC-0.1 sessionId 前缀不一致

**状态**：跳过（实际代码已统一使用 session_，无需修复）

---

## TC-0.2 UserController 冗余 import

**文件**：src/main/java/org/example/aispringboot/controller/User.java

**修改**：删除 import lombok.Builder;

**说明**：Controller 类上未使用 @Builder 注解，该 import 是多余的。

---

## TC-0.3 ConsultationSession 错误 import + 缺少构造器注解

**文件**：src/main/java/org/example/aispringboot/entity/ConsultationSession.java

**修改**：
1. 删除 import org.springframework.data.relational.core.mapping.Table;（Spring Data JDBC 的 Table，与 MyBatis-Plus 不兼容）
2. 新增 import lombok.NoArgsConstructor; 和 import lombok.AllArgsConstructor;
3. 类声明新增 @NoArgsConstructor 和 @AllArgsConstructor 注解

**说明**：补全注解后与 User entity 保持一致，避免 MyBatis-Plus 某些场景下因缺少无参构造导致实例化失败。另外原文件的中文注释因编码问题已损坏，替换为英文注释。

---

## TC-0.4 createSession() 返回 null

**文件**：src/main/java/org/example/aispringboot/service/ConsultationSessionService.java

**修改**：
1. 新增 import org.example.aispringboot.common.ResultCode;
2. 新增 import org.example.aispringboot.exception.BusinessException;
3. eturn null; → 	hrow new BusinessException(ResultCode.USER_NOT_EXIST.getMsg());

**说明**：当用户不存在时抛出 BusinessException（code=6002），由全局异常处理器统一拦截返回，替代原先返回 null 的做法。

---

## TC-0.5 PesponseUtil 拼写错误 + Tomcat import

**文件**：
- src/main/java/org/example/aispringboot/util/PesponseUtil.java → 删除
- src/main/java/org/example/aispringboot/util/ResponseUtil.java → 新建
- src/main/java/org/example/aispringboot/util/JwtAuthticationFilter.java → 修改

**修改**：
1. PesponseUtil.java 重命名为 ResponseUtil.java，类名同步修正
2. JwtAuthticationFilter 中删除错误 import org.apache.tomcat.util.http.ResponseUtil
3. JwtAuthticationFilter 中 3 处 PesponseUtil.writeError(...) 替换为 ResponseUtil.writeError(...)

---

## TC-0.6 SecurityConfig 白名单太窄

**文件**：src/main/java/org/example/aispringboot/config/SecurityConfig.java

**修改**：PUBLIC_PATHS 数组新增 3 个路径：
- /api/knowledge/article/page
- /api/knowledge/article/*
- /uploads/**

**修改前**：4 个路径（/、/api/test、/api/user/login、/api/user/add）
**修改后**：7 个路径

---

## 附加说明

- 修复过程中发现原文件使用 GBK 编码且含中文注释，PowerShell 管道编辑后 BOM/编码不匹配导致编译失败，已通过 [System.Text.UTF8Encoding]::new(False) 统一为 UTF-8 无 BOM 重新写入。
- TC-0.7（Token header 命名不一致）为注意项，暂未修复；HTTP header 名称大小写不敏感且 Controller 中优先从 Filter 缓存取值，不影响功能。
# 文件上传 403 - 问题分析与修复方案

## 1. 问题现象

管理员后台创建/编辑文章时上传封面图片：

```
POST http://localhost:5173/api/file/upload 403 (Forbidden)
```

## 2. 根因分析

### 2.1 接口存在性确认

`FileUploadController`（路径 `/api/file`）**存在** `@PostMapping("/upload")` 映射，接口本身已实现。

### 2.2 认证路径检查

| 路径 | PUBLIC_PATHS 匹配 | 结果 |
|------|------------------|------|
| `/api/file/upload` | 不在 PUBLIC_PATHS 中 | 需要 JWT 认证 |
| `/uploads/**` | 在 PUBLIC_PATHS 中 | 静态资源可公开访问 |

`/api/file/upload` 不在 `SecurityConfig.PUBLIC_PATHS` 中，必须通过 JWT 认证。

### 2.3 可能的原因分析

根据错误类型（403 而非 401），JwtAuthticationFilter 的可能返回码：

| 过滤器分支 | HTTP 状态码 | 触发条件 |
|-----------|------------|---------|
| TOKEN_ACCESS_FORBIDDEN | **403** | 用户状态非 NORMAL |
| ACCESS_UNAUTHORIZED | 401 | 无 token 头 |
| TOKEN_INVALID | 401 | token 验证失败 |

**403 最可能的原因**：Admin 用户状态不是 NORMAL。建议检查数据库中 admin 用户的 status 字段。

### 2.4 次要可能性

multipart/form-data 请求的 token header 可能被浏览器或代理过滤。Frontend `admin.js` 中 `uploadFile` 调用：

```javascript
return service.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
})
```

Axios 拦截器会在 `config.headers` 上添加 `token`，理论上不受 Content-Type 影响。

### 2.5 前端请求链路

```
admin.js: uploadFile(file, businessInfo)
  → POST /api/file/upload (multipart/form-data)
  → Axios interceptors add header: "token" = localStorage.getItem("token")
  → Vite proxy: localhost:5173 → localhost:1236
  → Spring Security JwtAuthticationFilter: extractTokenFromRequest()
  → check user status → TOKEN_ACCESS_FORBIDDEN (403) or pass
```

## 3. 修复步骤

### Step 1：排查数据库

```sql
SELECT id, username, user_type, status FROM user WHERE username = 'admin';
```

确认 admin 用户 `status = 1`（NORMAL）。

### Step 2：如果 status 正常

在后端 `JwtAuthticationFilter.doFilterInternal()` 方法添加 debug 日志：

```java
System.out.println("=== JWT Filter Debug ===");
System.out.println("Request URI: " + requestUri);
System.out.println("Token present: " + StringUtils.hasText(token));
System.out.println("User: " + JSONUtil.parseObj(user));
System.out.println("User status: " + (user != null ? user.getStatus() : "null"));
```

观察 `/api/file/upload` 请求进入 filter 时 token 和 user 信息是否正确。

### Step 3：检查 multipart 请求是否影响 token 提取

确认 `request.getHeader("token")` 在 multipart/form-data 请求中能正常获取。如果不能，可能是 Spring 的 `MultipartFilter` 或 `HiddenHttpMethodFilter` 影响了 request 的 header 读取顺序。

尝试将 token 改为 `Authorization: Bearer <token>` 标准格式：
1. 前端修改 `config.headers['Authorization'] = 'Bearer ' + token`
2. 后端修改 `extractTokenFromRequest()` 支持 Bearer 格式

### Step 4：临时方案 —— 加入 PUBLIC_PATHS

如果需要在修复前快速恢复功能，可以临时将文件上传加入公开路径：

```java
// SecurityConfig.java PUBLIC_PATHS 添加
"/api/file/upload"
```

**注意**：这会跳过 JWT 认证，仅作为临时诊断方案，修复后应立即移除。

### Step 5：长期方案 —— 前端改为 Bearer Token

统一使用 Authorization header 而非自定义 token header：

1. `request.js` interceptor 改为 `config.headers['Authorization'] = 'Bearer ' + token`
2. All backend filters 统一从 Authorization header 提取

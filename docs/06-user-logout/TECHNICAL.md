## 模块6：用户登出 · 技术文档

### 1. 接口清单（1 个）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 17 | POST | /api/user/logout | 用户登出 | 需登录 |

### 2. 新增/修改文件清单

| 文件 | 路径 |
|------|------|
| 修改 Controller | controller/User.java（追加 logout 方法） |

### 3. 核心实现

#### 3.1 Controller 追加方法

`java
@PostMapping("/logout")
public Result<Void> logout() {
    // JWT 无状态，服务端无需额外操作
    // 前端清除 token 后即完成登出
    // 此接口主要为满足前端调用规范
    return Result.ok();
}
`

### 4. 设计说明

- JWT 为无状态令牌，不存在服务端 session 需要销毁
- 本接口主要为了满足前端 dmin.js 中 logout() 调用
- 如果后续需要实现 token 黑名单（如强制下线），可在此接口将当前 token 加入 Redis 黑名单，JWT Filter 中校验黑名单
- 当前阶段保持简单实现

### 5. 后续可扩展

如果实现 token 黑名单：
1. 引入 Redis
2. logout() 时将当前 JWT jti 存入 Redis，TTL = token 剩余有效期
3. JwtAuthticationFilter 校验时额外检查 Redis 黑名单
4. 登录时也可实现"踢掉旧设备"

### 6. 注意事项

- 当前实现下，token 过期前仍可继续使用（JWT 无状态特性）
- 如需即时失效，必须实现黑名单机制
- 前端 equest.js 拦截器的 401 处理已存在，登出后前端自动跳转登录页

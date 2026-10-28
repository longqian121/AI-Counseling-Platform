# 模块6：用户登出 · 修改记录

## 修改日期
2026-07-18

## 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/main/java/org/example/aispringboot/controller/User.java` | 修改 | 追加 `logout()` 方法 |

## 代码变更详情

### controller/User.java

在 `getCurrentUser()` 方法之后、类闭合 `}` 之前，新增了 `logout()` 方法：

```java
// 用户登出
@PostMapping("/logout")
public Result<Void> logout() {
    // JWT 无状态，服务端无需额外操作；前端清除 token 后即完成登出
    return Result.ok();
}
```

## 测试结果

| 用例 | 结果 | 说明 |
|------|------|------|
| TC-6.1 正常登出 | ✓ PASS | POST /api/user/logout 返回 200, `{"code":"200","msg":"操作成功"}` |
| TC-6.2 未登录调用 | ✓ PASS | 无 token 时返回 401, JWT filter 正确拦截 |
| TC-6.3 登出后 token 仍可用 | ✓ PASS | 登出后用旧 token 访问 `/api/user/current` 仍返回用户数据，符合 JWT 无状态设计 |
| TC-6.4 前端 logout() 调用 | ✓ PASS | 后端接口已就绪，前端 `admin.js` 中的 `logout()` 调用链路打通 |

## 注意事项

- 当前实现为纯无状态登出，不销毁 token
- 登出后 token 在过期前仍可继续使用（JWT 无状态特性）
- 如需即时失效（如强制下线），需后续引入 Redis 黑名单机制

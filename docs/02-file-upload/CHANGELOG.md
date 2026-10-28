# 模块2：文件上传 · 变更记录

> 2026-07-18

## 新增文件

| 文件 | 路径 |
|---|---|
| Entity | `src/main/java/org/example/aispringboot/entity/SysFileInfo.java` |
| Mapper | `src/main/java/org/example/aispringboot/mapper/SysFileInfoMapper.java` |
| Controller | `src/main/java/org/example/aispringboot/controller/FileUploadController.java` |
| Config | `src/main/java/org/example/aispringboot/config/WebMvcConfig.java` |

## 修改文件

| 文件 | 变更 |
|---|---|
| `src/main/resources/application.yml` | 新增 `server.servlet.multipart` 配置（max-file-size: 10MB）和 `app.upload` 配置（path, allowed-types） |
| `src/main/java/org/example/aispringboot/common/GlobarExceptionHandler.java` | 新增 `MaxUploadSizeExceededException` 异常处理器，返回友好提示 |
| `ai-vue/src/components/ArticleDialog.vue` | `1014` → `1024`，修复前端文件大小校验 |


## 接口

| Method | Path | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/api/file/upload` | 通用文件上传（multipart/form-data） | 需登录 |

### 请求参数

| 字段 | 类型 | 必填 |
|---|---|---|
| file | File | 是 |
| businessType | String | 是 |
| businessId | Long | 否 |
| businessField | String | 否 |

### 响应

```json
{
  "code": "200",
  "data": {
    "fileName": "xxx.jpg",
    "filePath": "/uploads/article/2026/07/xxx.jpg",
    "fileSize": 102400
  }
}
```

## 实现要点

- 文件仅允许 `image/*` 类型，否则抛出 BusinessException
- 存储路径：`./uploads/{businessType}/{yyyy/MM}/{uuid}.{ext}`
- 上传后写入 `sys_file_info` 表
- `/uploads/**` 已在 SecurityConfig.PUBLIC_PATHS 中开放（无需修改）
- `WebMvcConfig` 配置静态资源映射，使上传文件可外部访问
- 超大文件（>10MB）由 `MaxUploadSizeExceededException` 统一处理

## 编译验证

- `mvn compile -DskipTests` → BUILD SUCCESS（无 clean）
- `mvn clean compile` 因已有的 `MyBatisPlusConfig.PaginationInnerInterceptor` 问题失败，非本模块引入
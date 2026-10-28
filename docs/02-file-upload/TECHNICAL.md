## 模块2：文件上传 · 技术文档
> **2026-07-18 修订**：
> - 数据库已有 sys_file_info 表，写入该表而非仅在本地存文件
> - 响应字段名改为 ilePath（匹配前端 ileRes.filePath），非 ileUrl
> - 前端 ileBaseUrl = http://localhost:1236（来自 config/index.js）
> - 前端 ArticleDialog.vue 的 1014→1024 需修复


### 1. 接口清单（1 个）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 8 | POST | `/api/file/upload` | 通用文件上传 | 需登录 |

### 2. 新增文件清单

| 文件 | 路径 |
|------|------|
| Controller | `controller/FileUploadController.java` |
| 配置 | `application.yml` 追加文件上传配置 |
| 配置 | `config/WebMvcConfig.java`（静态资源映射） |

### 3. 接口设计

#### 3.1 请求参数（multipart/form-data）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | 是 | 上传文件 |
| `businessType` | String | 是 | 业务类型，当前仅有 `ARTICLE` |
| `businessId` | String | 否 | 业务 ID（创建文章时可不传） |
| `businessField` | String | 否 | 业务字段名，如 `cover` |

#### 3.2 响应

```json
{
  "code": "200",
  "data": {
    "fileName": "abc123.jpg",
    "fileUrl": "/uploads/article/2026/07/abc123.jpg",
    "fileSize": 102400
  }
}
```

### 4. 核心实现

#### 4.1 application.yml 配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

app:
  upload:
    path: ./uploads       # 文件保存根目录（相对路径或绝对路径）
    allowed-types: image/jpeg,image/png,image/gif,image/webp
```

#### 4.2 Controller 核心逻辑

```java
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("businessType") String businessType,
            @RequestParam(value = "businessId", required = false) Long businessId,
            @RequestParam(value = "businessField", required = false) String businessField) {

        // 1. 文件校验
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 2. 类型校验（仅允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        // 3. 生成存储路径：uploads/{businessType}/{yyyy/MM}/{uuid}.{ext}
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = "/" + businessType.toLowerCase() + "/" + datePath + "/" + fileName;

        // 4. 保存文件
        Path targetPath = Paths.get(uploadPath, relativePath);
        Files.createDirectories(targetPath.getParent());
        file.transferTo(targetPath);

        // 5. 返回可访问的 URL
        String fileUrl = "/uploads" + relativePath;
        Map<String, Object> result = Map.of(
            "fileName", fileName,
            "fileUrl", fileUrl,
            "fileSize", file.getSize()
        );
        return Result.ok(result);
    }
}
```

#### 4.3 静态资源映射

在 `config/WebMvcConfig.java` 中配置上传文件可被外部访问：

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

**注意**：`/uploads/**` 路径需加入 `SecurityConfig.PUBLIC_PATHS`，否则上传后的图片无法被未登录用户访问（文章封面需公开）。

### 5. 安全配置补充

`SecurityConfig.PUBLIC_PATHS` 追加：
```java
"/uploads/**"
```

### 6. 文件大小校验（前端侧修复）

前端 `ArticleDialog.vue` 中 `1014` 改为 `1024`：
```js
// 修复前：
const isLt5M = file.size / 1014 / 1014 < 5
// 修复后：
const isLt5M = file.size / 1024 / 1024 < 5
```

### 7. 注意事项

- 使用 `Files.createDirectories()` 保证目录自动创建
- 文件名用 UUID 防冲突
- 不存储原始文件名到磁盘（安全考虑）
- 生产环境建议使用 OSS/CDN，本方案适合开发/测试阶段
- `businessType` 使用 `toLowerCase()` 统一路径格式

---
### 附录：sys_file_info DDL（已存在的文件管理表）

`sql
CREATE TABLE sys_file_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT '0',
    file_type VARCHAR(20) NOT NULL,
    business_type VARCHAR(50) DEFAULT NULL,
    business_id CHAR(255) DEFAULT NULL,
    business_field VARCHAR(50) DEFAULT NULL,
    upload_user_id BIGINT DEFAULT NULL,
    is_temp TINYINT(1) NOT NULL DEFAULT '0',
    status TINYINT(1) NOT NULL DEFAULT '1',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统文件信息表';
`

上传后写入此表，响应返回 ile_path（匹配前端 ileRes.filePath）。

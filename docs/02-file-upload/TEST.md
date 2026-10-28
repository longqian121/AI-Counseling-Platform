## 模块2：文件上传 · 测试文档
> **2026-07-18 修订**：后端返回 ilePath 而非 ileUrl；confirm sys_file_info 表写入


### TC-2.1 上传图片（正常）

| 项目 | 内容 |
|------|------|
| Method | POST `/api/file/upload` |
| Content-Type | multipart/form-data |
| 参数 | file=test.jpg, businessType=ARTICLE, businessId=, businessField=cover |
| 鉴权 | 需登录 |
| 预期 | 200，返回 `{ fileName, fileUrl: "/uploads/article/2026/07/xxx.jpg", fileSize }` |
| 验证点 | 文件存放在 `./uploads/article/2026/07/` 下，文件名 UUID |

### TC-2.2 通过 URL 访问上传文件

| 项目 | 内容 |
|------|------|
| Method | GET `/uploads/article/2026/07/xxx.jpg` |
| 鉴权 | **无**（公开） |
| 预期 | 200，浏览器显示图片 |

### TC-2.3 上传非图片文件

| 项目 | 内容 |
|------|------|
| 参数 | file=test.pdf, businessType=ARTICLE |
| 预期 | `BusinessException`，"仅支持上传图片文件" |

### TC-2.4 上传空文件

| 项目 | 内容 |
|------|------|
| 参数 | file=空文件 |
| 预期 | `BusinessException`，"文件不能为空" |

### TC-2.5 上传超大文件（>10MB）

| 项目 | 内容 |
|------|------|
| 参数 | file=15MB 图片 |
| 预期 | Spring 抛出 `MaxUploadSizeExceededException`，全局异常处理返回友好提示 |

### TC-2.6 前端文件大小校验修复

| 项目 | 内容 |
|------|------|
| 前置条件 | 前端 `ArticleDialog.vue` 中 `1014` 已改为 `1024` |
| 步骤 | 选择 4.9MB 图片 |
| 预期 | 前端校验通过（< 5MB），可以上传 |
| 步骤 | 选择 5.1MB 图片 |
| 预期 | 前端校验拦截，"图片大小不能超过 5MB" |

### TC-2.7 上传后图片可直接在文章封面显示

| 项目 | 内容 |
|------|------|
| 步骤 | 1. 上传图片获取 fileUrl<br>2. 创建文章时传入 coverImage=fileUrl<br>3. 访问文章列表，封面图正常渲染 |
| 预期 | 封面图 URL 可访问，无 401/404 |

## 模块1：知识文章 · 测试文档

> **2026-07-18 修订**：id 为 UUID 字符串；senderType 为 Integer(1/2) 非字符串


### 测试数据准备

```sql
-- 分类数据
INSERT INTO knowledge_category (id, category_name, parent_id, sort_order) VALUES
(1, '情绪管理', NULL, 1),
(2, '人际关系', NULL, 2),
(3, '焦虑应对', 1, 1),
(4, '抑郁疏导', 1, 2);
```

---

### TC-1.1 分类树查询

| 项目 | 内容 |
|------|------|
| Method | GET `/api/knowledge/category/tree` |
| 鉴权 | 需登录 |
| 预期响应 | `[{ "id":1, "categoryName":"情绪管理", "children":[{ "id":3, "categoryName":"焦虑应对" },{ "id":4, "categoryName":"抑郁疏导" }] }, { "id":2, "categoryName":"人际关系", "children":[] }]` |
| 验证点 | 树形结构正确，按 sort_order 排序 |

### TC-1.2 创建文章（草稿）

| 项目 | 内容 |
|------|------|
| Method | POST `/api/knowledge/article` |
| 鉴权 | 需登录 |
| 请求体 | `{ "title":"测试文章", "content":"<p>内容</p>", "summary":"摘要", "categoryId":1, "authorName":"管理员" }` |
| 预期响应 | 200，返回 `data: { "id": 1, "status": 0 }` |
| 验证点 | status 默认为 0，readCount 为 0，publishedAt 为 null |

### TC-1.3 创建文章（直接发布）

| 项目 | 内容 |
|------|------|
| Method | POST `/api/knowledge/article` |
| 请求体 | `{ ..., "status": 1 }` |
| 预期 | status=1，publishedAt 非空 |

### TC-1.4 公开分页查询

| 项目 | 内容 |
|------|------|
| Method | GET `/api/knowledge/article/page?page=1&pageSize=10` |
| 鉴权 | **无**（公开） |
| 预期 | 返回已发布文章列表，records 含 title/summary/coverImage/authorName/readCount/publishedAt |
| 验证点 | 草稿文章不在结果中（如前端不传 status 筛选时需确认） |

> **注意**：前端 `knowledge.vue`（管理端）传 `status` 筛选；`frontendKnowledge.vue`（用户端）只传 `status=1`。后端不做默认 status 过滤，由前端控制。

### TC-1.5 文章详情（公开）

| 项目 | 内容 |
|------|------|
| Method | GET `/api/knowledge/article/1` |
| 鉴权 | **无**（公开） |
| 预期 | 返回文章完整信息 |
| 验证点 | 每次请求后 `read_count` 自增 1 |

### TC-1.6 更新文章

| 项目 | 内容 |
|------|------|
| Method | PUT `/api/knowledge/article/1` |
| 鉴权 | 需登录 |
| 请求体 | `{ "title":"更新标题" }` |
| 预期 | 200，title 更新为"更新标题"，其他字段不变 |

### TC-1.7 发布文章

| 项目 | 内容 |
|------|------|
| Method | PUT `/api/knowledge/article/1/status` |
| 请求体 | `{ "status": 1 }` |
| 预期 | status=1，publishedAt 设置为当前时间 |

### TC-1.8 下线文章

| 项目 | 内容 |
|------|------|
| Method | PUT `/api/knowledge/article/1/status` |
| 请求体 | `{ "status": 2 }` |
| 预期 | status=2 |

### TC-1.9 删除文章

| 项目 | 内容 |
|------|------|
| Method | DELETE `/api/knowledge/article/1` |
| 鉴权 | 需登录 |
| 预期 | 200，数据库记录删除 |

### TC-1.10 边界条件

| 场景 | 预期 |
|------|------|
| 标题为空 | 400，校验失败 |
| categoryId 不存在 | 200（不校验外键完整性） |
| 更新不存在的文章 | `BusinessException`，文章不存在 |
| 分页 page=99999 | 返回空 records |
| status 传 99 | 400，状态值非法 |

# 知识文章 CRUD 接口缺失 - 问题分析与修复方案

## 1. 问题现象

管理员后台 "知识文章" 模块以下操作全部返回 403：

| 操作 | 前端请求 | 状态 |
|------|----------|------|
| 新增文章 | POST /api/knowledge/article | 403 |
| 文章详情 | GET /api/knowledge/article/{id} | 403 |
| 编辑文章 | PUT /api/knowledge/article/{id} | 403 |
| 修改状态 | PUT /api/knowledge/article/{id}/status | 403 |
| 删除文章 | DELETE /api/knowledge/article/{id} | 403 |

## 2. 根因分析

### 2.1 后端现状

`KnowledgeArticleController`（路径 `/api/knowledge`）只实现了 1 个接口：

- `GET /category/tree` —— 分类树 ✅

**缺少以下全部接口：**

- `POST /article` —— 创建文章
- `GET /article/{id}` —— 文章详情
- `PUT /article/{id}` —— 更新文章
- `PUT /article/{id}/status` —— 修改文章状态
- `DELETE /article/{id}` —— 删除文章

### 2.2 为什么是 403 而不是 404

`SecurityConfig.PUBLIC_PATHS` 中包含 `/api/knowledge/article/*` 和 `/api/knowledge/article/page`，但 AntPathMatcher 的 `*` 只匹配**单个路径段**。`POST /api/knowledge/article`（无尾部路径段）不匹配 `/*`，也不在 PUBLIC_PATHS 中。请求经过 JWT 认证后进入 Spring Security 的 AuthorizationFilter，找不到对应的 Controller 映射，返回 403。

### 2.3 已可用的接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 文章分页列表 | GET /api/knowledge/article/page | 在 PUBLIC_PATHS 中，可公共访问 |
| 分类树 | GET /api/knowledge/category/tree | 已实现 |

### 2.4 关联 Service

`KnowledgeArticleService` 目前只有 `getCategoryTree()` 方法，不包含任何文章 CRUD 逻辑。无 Article 实体、无 ArticleMapper。

## 3. 前端数据结构

### 3.1 文章表单（ArticleDialog.vue）

```javascript
{
  title: "",        // 文章标题
  content: "",      // 富文本 HTML
  coverImage: "",   // 封面图片路径
  categoryId: "",   // 分类 ID
  summary: "",      // 摘要
  tags: "",         // 标签（逗号分隔字符串）
  id: ""            // 编辑时传文章 ID
}
```

### 3.2 文章状态变更

```javascript
changeArticleStatus(id, { status: 0 })  // 0-草稿 1-发布
```

## 4. 修复步骤

### Step 1：创建 Article 实体

`src/main/java/.../entity/KnowledgeArticle.java`

```java
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String coverImage;
    private Long categoryId;
    private String summary;
    private String tags;
    private Integer status;       // 0-草稿 1-发布
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Step 2：创建 ArticleMapper

```java
@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {
    @Select("SELECT a.*, u.nickname AS authorName, c.category_name AS categoryName "
          + "FROM knowledge_article a "
          + "LEFT JOIN user u ON a.author_id = u.id "
          + "LEFT JOIN knowledge_category c ON a.category_id = c.id "
          + "ORDER BY a.created_at DESC")
    IPage<KnowledgeArticle> selectArticlePage(Page<?> page);
}
```

### Step 3：在 KnowledgeArticleService 中添加方法

- `getArticlePage(pageNum, pageSize)` —— 分页查询（含用户和分类信息）
- `getArticleById(id)` —— 详情
- `createArticle(dto, userId)` —— 创建
- `updateArticle(id, dto)` —— 更新
- `updateArticleStatus(id, status)` —— 改状态
- `deleteArticle(id)` —— 删除

### Step 4：在 KnowledgeArticleController 中添加端点

- `GET /article/{id}` —— 详情
- `POST /article` —— 创建
- `PUT /article/{id}` —— 更新
- `PUT /article/{id}/status` —— 状态变更
- `DELETE /article/{id}` —— 删除

### Step 5：数据库建表

```sql
CREATE TABLE IF NOT EXISTS knowledge_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    cover_image VARCHAR(500),
    category_id BIGINT,
    summary VARCHAR(500),
    tags VARCHAR(500),
    status INT DEFAULT 1 COMMENT '0-草稿 1-发布',
    author_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_id (category_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5. 注意事项

- 所有增删改接口需要 JWT 认证（不在 PUBLIC_PATHS 中）
- 创建/编辑时 authorId 从 JWT token 中提取当前登录用户
- 列表查询需要 JOIN user 表获取作者名、JOIN category 表获取分类名
- 删除文章时应考虑是否同步删除关联的封面图片文件

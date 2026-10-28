## 模块1：知识文章 · 技术文档

> **数据库验证日期**：2026-07-18 — 基于 `SHOW CREATE TABLE` 实际结构
> **关键差异 vs 原文档**：id 为 VARCHAR(36) UUID；author_id(FK) 替代 author_name；status 仅 1=已发布；summary 为 TEXT


### 1. 接口清单（7 个）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 1 | GET | `/api/knowledge/category/tree` | 分类树 | 需登录 |
| 2 | GET | `/api/knowledge/article/page` | 文章分页 | 公开 |
| 3 | POST | `/api/knowledge/article` | 创建文章 | 需登录 |
| 4 | GET | `/api/knowledge/article/{id}` | 文章详情 | 公开 |
| 5 | PUT | `/api/knowledge/article/{id}` | 更新文章 | 需登录 |
| 6 | PUT | `/api/knowledge/article/{id}/status` | 发布/下线 | 需登录 |
| 7 | DELETE | `/api/knowledge/article/{id}` | 删除文章 | 需登录 |

### 2. SQL DDL

```sql
CREATE TABLE knowledge_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT,
    summary VARCHAR(1000),
    cover_image VARCHAR(500),
    category_id BIGINT,
    author_name VARCHAR(100),
    tags VARCHAR(500),
    status TINYINT DEFAULT 0 COMMENT '0-草稿 1-已发布 2-已下线',
    read_count INT DEFAULT 0,
    published_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3. 新增文件清单

| 文件 | 路径 |
|------|------|
| Entity | `entity/KnowledgeCategory.java` |
| Entity | `entity/KnowledgeArticle.java` |
| Mapper | `mapper/KnowledgeCategoryMapper.java` |
| Mapper | `mapper/KnowledgeArticleMapper.java` |
| Service | `service/KnowledgeArticleService.java` |
| Controller | `controller/KnowledgeArticleController.java` |
| DTO | `DTO/command/ArticleCreateDTO.java`（含 update 复用） |
| DTO | `DTO/command/ArticleStatusDTO.java` |
| DTO | `DTO/response/ArticlePageResponseDTO.java` |
| DTO | `DTO/response/CategoryTreeDTO.java` |

### 4. 核心实现要点

#### 4.1 KnowledgeCategory 实体

```java
@Data
@TableName("knowledge_category")
public class KnowledgeCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 树形 DTO 转换辅助
    @TableField(exist = false)
    private List<KnowledgeCategory> children;
}
```

#### 4.2 KnowledgeArticle 实体

```java
@Data
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private Long categoryId;
    private String authorName;
    private String tags;
    private Integer status;
    private Integer readCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 4.3 GET `/category/tree` — 分类树

1. 查询所有 `knowledge_category` 记录
2. 内存构建树形结构：`parentId == null` 为根节点，递归挂载子节点
3. 按 `sort_order` 升序排序

#### 4.4 GET `/article/page` — 文章分页

1. 使用 MyBatis-Plus `Page<T>` + `LambdaQueryWrapper`
2. 支持查询参数：`title`（LIKE）、`categoryId`、`status`
3. 排序：`published_at DESC, created_at DESC`
4. 响应结构：
```json
{
  "records": [{ "id":1, "title":"...", "summary":"...", "coverImage":"...", "authorName":"...", "status":1, "readCount":100, "publishedAt":"...", "categoryId":1 }],
  "total": 50,
  "page": 1,
  "pageSize": 10
}
```

**注意**：此接口为公开接口，需在 `SecurityConfig.PUBLIC_PATHS` 中加入 `"/api/knowledge/article/page"`。

#### 4.5 POST `/article` — 创建文章

1. DTO 校验：`title` 非空、`content` 非空
2. 默认 `status = 0`（草稿）
3. `readCount = 0`
4. 如果传了 `status = 1`，同时设置 `publishedAt = LocalDateTime.now()`
5. 调用 `knowledgeArticleMapper.insert(article)` 返回新 ID

#### 4.6 GET `/article/{id}` — 文章详情

1. `knowledgeArticleMapper.selectById(id)`
2. 同时累加 `readCount + 1`（`knowledgeArticleMapper.update(null, Wrappers.lambdaUpdate...setSql("read_count = read_count + 1").eq(KnowledgeArticle::getId, id))`）

**注意**：此接口为公开接口，需在 `SecurityConfig.PUBLIC_PATHS` 中加入 `"/api/knowledge/article/*"`。

#### 4.7 PUT `/article/{id}` — 更新文章

1. DTO 校验同上
2. 查询现有文章，存在则更新字段
3. 如果 `status` 从草稿变成已发布，设置 `publishedAt`

#### 4.8 PUT `/article/{id}/status` — 发布/下线

1. 接收 `ArticleStatusDTO { status }`
2. `status` 校验：只允许 0/1/2
3. 如果 `status == 1`，设置 `publishedAt = LocalDateTime.now()`

#### 4.9 DELETE `/article/{id}` — 删除

1. `knowledgeArticleMapper.deleteById(id)`
2. 物理删除（前端无回收站需求）

### 5. Mapper 扩展

`KnowledgeArticleMapper` 继承 `BaseMapper<KnowledgeArticle>`：

```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {
}
```

### 6. 安全配置补充

在 `SecurityConfig.PUBLIC_PATHS` 追加：
```java
"/api/knowledge/article/page",
"/api/knowledge/article/*"
```

### 7. 注意事项

- `content` 字段存储富文本 HTML，不做 XSS 过滤（管理端可控输入）
- `tags` 字段为逗号分隔字符串，前端自行 split/join
- 分类树如果数据量大（>1000条），可加 Redis 缓存，本阶段先内存构建
- 删除文章不级联删除封面图片（文件模块独立管理）

---
### 附录：实际数据库 DDL（2026-07-18 验证）

重点差异：
- knowledge_article.id: VARCHAR(36) UUID 非自增 BIGINT
- knowledge_article.author_id: FK→user 非 author_name VARCHAR
- knowledge_article.status: 仅 1=已发布，无草稿/下线状态
- knowledge_article.summary: TEXT 非 VARCHAR(1000)
- knowledge_category 额外字段: category_code, description, status
- emotion_diary 已存在，含 UNIQUE(user_id, diary_date)
- sys_file_info 已存在，写入该表

# 任务 #1 修改记录 — GET /api/knowledge/category/tree 分类树

> **日期**：2026-07-18
> **任务范围**：仅实现接口清单 #1（分类树查询），不涉及 #2-#7

## 新增文件

| 文件 | 路径 | 说明 |
|------|------|------|
| KnowledgeCategory.java | `entity/KnowledgeCategory.java` | 分类实体，映射 `knowledge_category` 表 |
| KnowledgeCategoryMapper.java | `mapper/KnowledgeCategoryMapper.java` | MyBatis-Plus Mapper 接口 |
| CategoryTreeDTO.java | `DTO/response/CategoryTreeDTO.java` | 分类树响应 DTO |
| KnowledgeArticleService.java | `service/KnowledgeArticleService.java` | 文章服务（暂仅实现 `getCategoryTree`） |
| KnowledgeArticleController.java | `controller/KnowledgeArticleController.java` | 文章控制器（暂仅实现 #1 分类树端点） |

## 实现要点

- **实体字段**：id, categoryName, parentId, sortOrder, createdAt, updatedAt, children(transient)
- **children** 初始化为 `new ArrayList<>()` 防止 NPE
- **树构建**：`selectList(null)` → 按 `sortOrder` 排序 → 内存构建父子关系 → 过滤 root（`parentId == null || parentId == 0L`）
- **端点**：`GET /api/knowledge/category/tree`，需登录（JWT 验证），不在 PUBLIC_PATHS 中
- **与文档差异**：实际数据库 `parent_id = 0` 表示根节点（非 NULL），代码兼容两种写法

## 测试结果（TC-1.1）

| 验证点 | 结果 |
|--------|------|
| 未登录返回 401 | ✅ `{"code":"A0301","msg":"访问未授权"}` |
| 树形结构正确 | ✅ 心理健康基础 → [人际关系, 压力缓解]，情绪管理 |
| 按 sort_order 排序 | ✅ 人际关系(10) < 压力缓解(20)，心理健康基础(30) < 情绪管理(40) |

## 依赖说明

- MyBatis-Plus 3.5.15 自动配置，无额外 XML
- 使用项目现有模式：`@Resource` 注入、`@Service`、`@RestController`
- 未修改 `SecurityConfig`（此接口需登录，不加入 PUBLIC_PATHS）
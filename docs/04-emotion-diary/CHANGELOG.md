# 模块4：情绪日记 · 变更记录

> 任务：#13 POST /api/emotion-diary — 创建情绪日记
> 日期：2026-07-18

## 新增文件

| 文件 | 路径 | 说明 |
|------|------|------|
| Entity | `src/main/java/org/example/aispringboot/entity/EmotionDiary.java` | 情绪日记实体，映射 emotion_diary 表 |
| DTO | `src/main/java/org/example/aispringboot/DTO/command/EmotionDiaryCreateDTO.java` | 创建日记请求 DTO，含 @NotNull/@Min/@Max 注解 |
| Mapper | `src/main/java/org/example/aispringboot/mapper/EmotionDiaryMapper.java` | MyBatis-Plus BaseMapper |
| Service | `src/main/java/org/example/aispringboot/service/EmotionDiaryService.java` | 创建/更新（upsert）+ 显式参数校验 |
| Controller | `src/main/java/org/example/aispringboot/controller/EmotionDiaryController.java` | POST /api/emotion-diary，JWT 提取 userId |
| Schema | `src/main/resources/schema.sql` | emotion_diary 建表 DDL（CREATE TABLE IF NOT EXISTS） |

## 修改的已有文件

| 文件 | 修改内容 | 原因 |
|------|---------|------|
| `config/MyBatisPlusConfig.java` | 移除 `PaginationInnerInterceptor` 导入和注册 | 编译错误：mybatis-plus 3.5.15 已移除该类，分页功能内置在 MybatisPlusInterceptor 中 |
| `resources/application.yml` | 添加 `spring.sql.init.mode: always` | 启用 schema.sql 自动建表 |
| `pom.xml` | 添加 `<mainClass>` 配置到 spring-boot-maven-plugin | 修复 `Unable to find a suitable main class` 启动错误 |

## 核心逻辑

1. **鉴权**：从 JWT token（header: `token`）中提取 userId，不从请求体获取
2. **Upsert**：同一用户同一天（userId + diaryDate）查已有记录 → 存在则更新，不存在则新增
3. **日期默认**：diaryDate 不传则默认当天
4. **参数校验**：Service 层显式校验 moodScore(1-10)、sleepQuality(1-5|null)、stressLevel(1-5|null)
5. **ai_emotion_analysis**：当前阶段传 null，预留后续 AI 异步填充

## 测试结果

| 用例 | 结果 |
|------|------|
| TC-4.1 创建情绪日记 | ✅ PASS |
| TC-4.2 同一天重复创建 (upsert) | ✅ PASS |
| TC-4.3 评分范围校验 (moodScore/sleepQuality/stressLevel) | ✅ PASS |

## 已知问题

- `@Valid` DTO 注解校验在当前 Spring Boot 3.5.16 下未生效（原因待查），改用 Service 层显式校验确保安全性
- 框架中 BusinessException 返回 HTTP 200 + code 400（非 HTTP 400），与项目已有 User 模块行为一致

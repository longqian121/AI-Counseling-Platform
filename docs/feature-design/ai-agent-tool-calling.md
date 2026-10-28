# AI Agent 工具调用 —— 技术设计文档

## 1. 功能概述

在心理支持聊天中集成 AI Agent，让用户通过自然语言触发后端操作。例如：

> 用户：帮我预约周五下午的心理咨询。
> AI：好的，周五（7月25日）下午 14:00-15:00 的李医生可以吗？
> 用户：可以。
> AI：已为您预约成功，7月25日 14:00 李医生，届时请准时参加。

核心技术：Spring AI 的 `@Tool` 注解 + DeepSeek 的 function calling 能力。

## 2. 为什么这个功能值得做

- Spring AI 对 function calling 的封装非常成熟，`@Tool` 一个注解搞定
- 项目已有 `ChatClient` 集成，只需加入 tool 注册
- Agent 是 AI 面试的高频考点，"实现了 AI Agent 工具调用"一行够打
- 预约是真业务场景，不是 demo 级玩具

## 3. 涉及的角色

目前项目只有"用户(user)"和"管理员(admin)"两个角色（通过 `user_type` 字段区分：1=普通用户，2=管理员）。需要新增一个角色：

| 角色 | user_type | 说明 |
|------|-----------|------|
| 普通用户 | 1（已有） | 使用心理支持聊天、预约咨询 |
| 心理医生 | 3（新增） | 接收预约、查看咨询记录 |
| 管理员 | 2（已有） | 管理系统所有资源 |

## 4. 新增实体

### 4.1 Counselor（心理医生）

```java
@TableName("counselor")
public class Counselor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;           // 姓名
    private String title;          // 职称（如"高级心理咨询师"）
    private String specialty;      // 擅长领域（如"焦虑症、抑郁症、婚姻咨询"）
    private String avatar;         // 头像
    private String description;    // 个人简介
    private Integer status;        // 0-停诊 1-可预约
    private LocalDateTime createdAt;
}
```

### 4.2 Appointment（预约记录）

```java
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;           // 预约用户
    private Long counselorId;      // 心理医生
    private LocalDate appointmentDate;  // 预约日期
    private String timeSlot;       // 时间段（如"14:00-15:00"）
    private Integer status;        // 0-已取消 1-已预约 2-已完成
    private String remark;         // 用户备注（如"最近失眠严重"）
    private String cancelReason;   // 取消原因
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 5. Tool 函数定义

项目已有的 `FunctionCallingAgentService.java` 需要按以下方式改造。核心是利用 Spring AI 的 `@Tool` 注解注册工具函数：

```java
@Service
public class AgentToolService {

    @Resource private CounselorMapper counselorMapper;
    @Resource private AppointmentMapper appointmentMapper;
    @Resource private ConsultationSessionMapper consultationSessionMapper;

    // ========== 预约相关 ==========

    @Tool(description = "查询可预约的时间段，返回某天某个医生的空闲时段列表")
    public String getAvailableSlots(Long counselorId, String date) {
        // 1. 查该医生当天已预约的时段
        // 2. 排除已占用，返回可预约列表
    }

    @Tool(description = "为用户预约心理咨询，需要指定医生ID、日期、时间段")
    public String bookAppointment(Long userId, Long counselorId, String date, String timeSlot, String remark) {
        // 1. 校验医生是否可预约(status=1)
        // 2. 校验时间段是否冲突
        // 3. 插入 appointment 记录
        // 4. 返回预约成功信息
    }

    @Tool(description = "取消已有的心理咨询预约")
    public String cancelAppointment(Long appointmentId, String reason) {
        // 更新 status=0，记录取消原因
    }

    @Tool(description = "查询用户的预约记录")
    public String getMyAppointments(Long userId) {
        // 返回该用户的所有预约（含医生信息）
    }

    // ========== 咨询历史 ==========

    @Tool(description = "查询用户的历史咨询会话列表")
    public String getConsultationHistory(Long userId) {
        // 返回最近的咨询会话摘要
    }

    @Tool(description = "查询指定会话的对话记录")
    public String getSessionMessages(Long sessionId) {
        // 返回该会话的所有消息
    }
}
```

## 6. 集成到聊天流程

修改 `PsychologicalSupportService.streamPsychologicalChat()`，在 ChatClient 调用时注册 tools：

```java
chatClient.prompt(prompt)
    .user(userMessage)
    .tools(agentToolService)  // 注册工具函数
    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
    .stream()
    .content()
    // ... 后续处理不变
```

DeepSeek 在收到用户消息后，如果检测到意图匹配 tool，会自动返回 function call 请求。Spring AI 会本地执行对应方法，将结果回传给 DeepSeek，最终生成带工具调用结果的回复。

## 7. 管理员端 CRUD

心理医生管理（管理员后台）：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/admin/counselor/page` | GET | 分页列表 |
| `/api/admin/counselor` | POST | 新增医生 |
| `/api/admin/counselor/{id}` | PUT | 编辑医生 |
| `/api/admin/counselor/{id}` | DELETE | 删除医生 |
| `/api/admin/counselor/{id}/status` | PUT | 修改接诊状态 |

预约管理：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/admin/appointment/page` | GET | 分页列表 |
| `/api/admin/appointment/{id}` | GET | 详情 |
| `/api/admin/appointment/{id}/status` | PUT | 修改预约状态 |

## 8. 前端改动

- `admin.js` 新增 `counselorPage`、`createCounselor`、`updateCounselor` 等 API
- 新增 `CounselorManage.vue`（医生管理页，含表格 + 新增/编辑弹窗）
- 新增 `AppointmentManage.vue`（预约管理页）
- 左侧菜单新增"心理医生管理"和"预约管理"入口

## 9. 数据库建表

```sql
CREATE TABLE counselor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    title VARCHAR(100),
    specialty VARCHAR(500),
    avatar VARCHAR(255),
    description TEXT,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    counselor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    cancel_reason VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_counselor_id (counselor_id),
    INDEX idx_appointment_date (appointment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 10. 实施步骤

| 步骤 | 内容 | 预估时间 |
|------|------|----------|
| 1 | 建 counselor/appointment 实体 + Mapper | 0.5 天 |
| 2 | 实现 AgentToolService（@Tool 函数） | 0.5 天 |
| 3 | 集成到 PsychologicalSupportService 聊天流 | 0.25 天 |
| 4 | 管理员端 counselor/appointment CRUD | 0.5 天 |
| 5 | 前端医生管理 + 预约管理页面 | 0.5 天 |
| **合计** | | **2-2.5 天** |

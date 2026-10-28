CREATE TABLE IF NOT EXISTS emotion_diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    diary_date DATE NOT NULL,
    mood_score INT COMMENT '情绪评分 1-10',
    dominant_emotion VARCHAR(50) COMMENT '主要情绪',
    emotion_triggers TEXT COMMENT '情绪触发因素',
    diary_content TEXT COMMENT '日记内容',
    sleep_quality INT COMMENT '睡眠质量 1-5',
    stress_level INT COMMENT '压力水平 1-5',
    ai_emotion_analysis TEXT COMMENT 'AI 情绪分析结果 JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, diary_date),
    INDEX idx_user_id (user_id),
    INDEX idx_diary_date (diary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
CREATE TABLE IF NOT EXISTS counselor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(50) NOT NULL,
    title VARCHAR(100),
    specialty VARCHAR(500),
    avatar VARCHAR(255),
    description TEXT,
    status TINYINT DEFAULT 1 COMMENT '0-停诊 1-可预约',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    counselor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    status TINYINT DEFAULT 1 COMMENT '0-已取消 1-已预约 2-已完成',
    remark VARCHAR(500),
    cancel_reason VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_counselor_id (counselor_id),
    INDEX idx_appointment_date (appointment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS risk_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    risk_level TINYINT NOT NULL COMMENT '0=正常 1=关注 2=轻度 3=中度 4=高度',
    risk_type VARCHAR(50) COMMENT '自杀风险/自伤风险/危机事件/无',
    confidence INT COMMENT '置信度 0-100',
    evidence TEXT COMMENT '识别到的关键语句',
    suggested_action TEXT COMMENT '建议措施',
    is_read TINYINT DEFAULT 0 COMMENT '0=未读 1=已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pre_visit_briefing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    patient_user_id BIGINT NOT NULL,
    doctor_user_id BIGINT NOT NULL,
    patient_background TEXT COMMENT '患者背景画像',
    key_concerns TEXT COMMENT '核心心理问题归纳',
emotion_trend TEXT COMMENT '情绪变化趋势: 改善/稳定/恶化',
    risk_history TEXT COMMENT '历史风险告警摘要',
    suggested_approach TEXT COMMENT '建议医生沟通策略',
    is_viewed TINYINT DEFAULT 0 COMMENT '0=未查看 1=已查看',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_appointment_id (appointment_id),
    INDEX idx_patient_user_id (patient_user_id),
    INDEX idx_doctor_user_id (doctor_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quality_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_date DATE NOT NULL,
    total_sessions INT DEFAULT 0 COMMENT '昨日有消息的会话数',
    risk_alert_count INT DEFAULT 0 COMMENT '昨日风险告警数',
    crisis_count INT DEFAULT 0 COMMENT 'riskLevel>=3的会话数',
    empathy_score DECIMAL(4,2) COMMENT 'AI共情评分 1-10',
    relevance_score DECIMAL(4,2) COMMENT '回复相关性评分 1-10',
    safety_score DECIMAL(4,2) COMMENT '安全性评分 1-10',
    overall_score DECIMAL(4,2) COMMENT '综合评分',
    top_issues TEXT COMMENT '高频问题TOP5 JSON',
    flagged_session_ids TEXT COMMENT '需复核的会话ID列表',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

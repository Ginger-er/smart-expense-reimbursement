-- =============================================
-- 智能差旅报销系统 - 数据库初始化脚本
-- Database: smart_expense
-- =============================================

CREATE DATABASE IF NOT EXISTS smart_expense DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_expense;

-- =============================================
-- 部门表
-- =============================================
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父部门ID',
    dept_name   VARCHAR(64)  NOT NULL COMMENT '部门名称',
    leader_id   BIGINT       DEFAULT NULL COMMENT '部门负责人ID',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      TINYINT      DEFAULT 1 COMMENT '状态(1正常 0停用)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- =============================================
-- 用户表
-- =============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(32)  NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码',
    real_name   VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    dept_id     BIGINT       DEFAULT NULL COMMENT '部门ID',
    role        TINYINT      NOT NULL DEFAULT 1 COMMENT '角色: 1员工 2领导 3财务 4管理员',
    phone        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    status       TINYINT      DEFAULT 1 COMMENT '状态(1正常 0停用)',
    pwd_modified TINYINT      DEFAULT 0 COMMENT '密码是否已改(0初始密码 1已修改)',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 出差申请表
-- =============================================
DROP TABLE IF EXISTS trip;
CREATE TABLE trip (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id       BIGINT        NOT NULL COMMENT '申请人ID',
    trip_no       VARCHAR(32)   NOT NULL COMMENT '出差申请编号',
    destination   VARCHAR(128)  NOT NULL COMMENT '目的地',
    purpose       VARCHAR(512)  NOT NULL COMMENT '出差事由',
    start_date    DATE          NOT NULL COMMENT '开始日期',
    end_date      DATE          NOT NULL COMMENT '结束日期',
    budget_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '预算金额',
    status        TINYINT       DEFAULT 0 COMMENT '状态: 0草稿 1已提交 2审批中 3已通过 4已驳回',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_no (trip_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出差申请表';

-- =============================================
-- 发票表
-- =============================================
DROP TABLE IF EXISTS invoice;
CREATE TABLE invoice (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id          BIGINT        DEFAULT NULL COMMENT '上传人ID',
    trip_id          BIGINT        DEFAULT NULL COMMENT '出差申请ID',
    reimbursement_id BIGINT        DEFAULT NULL COMMENT '关联报销单ID',
    invoice_no       VARCHAR(32)   DEFAULT NULL COMMENT '发票号码',
    invoice_code     VARCHAR(32)   DEFAULT NULL COMMENT '发票代码',
    amount           DECIMAL(12,2) DEFAULT NULL COMMENT '发票金额, 识别前可为空',
    tax_amount       DECIMAL(12,2) DEFAULT 0.00 COMMENT '税额',
    invoice_date     DATE          DEFAULT NULL COMMENT '开票日期',
    type             TINYINT       NOT NULL COMMENT '类型: 1交通 2住宿 3餐饮 4其他',
    seller_name      VARCHAR(128)  DEFAULT NULL COMMENT '销售方名称',
    buyer_name       VARCHAR(128)  DEFAULT NULL COMMENT '购买方名称',
    file_url         VARCHAR(512)  DEFAULT NULL COMMENT '发票文件URL',
    ocr_json         TEXT          DEFAULT NULL COMMENT 'OCR识别结果JSON',
    ocr_status       TINYINT       DEFAULT 0 COMMENT 'OCR状态: 0待识别 1成功 2失败 3人工修正',
    verify_status    TINYINT       DEFAULT 0 COMMENT '校验状态: 0未校验 1通过 2失败',
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_trip_id (trip_id),
    KEY idx_reimbursement_id (reimbursement_id),
    KEY idx_invoice_no (invoice_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票表';

-- =============================================
-- 报销单表
-- =============================================
DROP TABLE IF EXISTS reimbursement;
CREATE TABLE reimbursement (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id             BIGINT        NOT NULL COMMENT '申请人ID',
    reimburse_no        VARCHAR(32)   NOT NULL COMMENT '报销单编号',
    total_amount        DECIMAL(12,2) DEFAULT 0.00 COMMENT '报销总金额',
    invoice_count       INT           DEFAULT 0 COMMENT '发票数量',
    status              TINYINT       DEFAULT 0 COMMENT '状态: 0草稿 1待审批 2审批中 3已通过 4已驳回 5已打款',
    reject_reason       VARCHAR(512)  DEFAULT NULL COMMENT '驳回原因',
    remark              VARCHAR(512)  DEFAULT NULL COMMENT '报销说明',
    pay_time            DATETIME      DEFAULT NULL COMMENT '打款时间',
    pay_user_id         BIGINT        DEFAULT NULL COMMENT '打款人ID',
    pay_user_name       VARCHAR(32)   DEFAULT NULL COMMENT '打款人姓名',
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reimburse_no (reimburse_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销单表';

-- =============================================
-- 审批记录表
-- =============================================
DROP TABLE IF EXISTS approval_record;
CREATE TABLE approval_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    trip_id         BIGINT       DEFAULT NULL COMMENT '出差申请ID',
    reimbursement_id BIGINT      DEFAULT NULL COMMENT '报销单ID',
    approver_id     BIGINT       NOT NULL COMMENT '审批人ID',
    action          TINYINT      NOT NULL COMMENT '操作: 1通过 2驳回 3转办',
    comment         VARCHAR(512) DEFAULT NULL COMMENT '审批意见',
    node_name       VARCHAR(64)  DEFAULT NULL COMMENT '审批节点名称',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_reimbursement_id (reimbursement_id),
    KEY idx_approver_id (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- =============================================
-- 操作日志表
-- =============================================
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id        BIGINT       DEFAULT NULL COMMENT '操作人ID',
    username       VARCHAR(32)  DEFAULT NULL COMMENT '操作人用户名',
    title          VARCHAR(64)  DEFAULT NULL COMMENT '操作标题',
    method         VARCHAR(128) DEFAULT NULL COMMENT '方法名',
    request_method VARCHAR(8)   DEFAULT NULL COMMENT '请求方式',
    request_url    VARCHAR(256) DEFAULT NULL COMMENT '请求地址',
    request_params VARCHAR(512) DEFAULT NULL COMMENT '请求参数',
    ip             VARCHAR(64)  DEFAULT NULL COMMENT '来源IP',
    status         TINYINT      DEFAULT 1 COMMENT '状态(1成功 0失败)',
    error_msg      VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    cost_ms        BIGINT       DEFAULT NULL COMMENT '耗时(毫秒)',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================================
-- 消息通知表
-- =============================================
DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人ID',
    title       VARCHAR(128) NOT NULL COMMENT '通知标题',
    content     VARCHAR(512) DEFAULT NULL COMMENT '通知内容',
    type        TINYINT      DEFAULT 1 COMMENT '类型: 1审批结果 2系统通知',
    link        VARCHAR(256) DEFAULT NULL COMMENT '跳转链接',
    is_read     TINYINT      DEFAULT 0 COMMENT '是否已读(0未读 1已读)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- =============================================
-- 初始数据（部门）
-- 用户数据由 DataInitializer 在应用启动时自动创建
-- =============================================
INSERT INTO sys_dept (id, parent_id, dept_name, leader_id, sort_order) VALUES
(1, 0, '总公司', NULL, 0),
(2, 1, '技术部', NULL, 1),
(3, 1, '市场部', NULL, 2),
(4, 1, '财务部', NULL, 3),
(5, 1, '人事部', NULL, 4);

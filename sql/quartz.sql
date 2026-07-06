-- ============================================================
-- Quartz 调度器 PostgreSQL DDL
-- 从 MySQL 迁移至 PostgreSQL
-- ============================================================

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

-- 1、存储每一个已配置的 jobDetail 的详细信息
CREATE TABLE QRTZ_JOB_DETAILS (
    sched_name           VARCHAR(120)    NOT NULL,
    job_name             VARCHAR(200)    NOT NULL,
    job_group            VARCHAR(200)    NOT NULL,
    description          VARCHAR(250)    NULL,
    job_class_name       VARCHAR(250)    NOT NULL,
    is_durable           VARCHAR(1)      NOT NULL,
    is_nonconcurrent     VARCHAR(1)      NOT NULL,
    is_update_data       VARCHAR(1)      NOT NULL,
    requests_recovery    VARCHAR(1)      NOT NULL,
    job_data             BYTEA           NULL,
    PRIMARY KEY (sched_name, job_name, job_group)
);
COMMENT ON TABLE QRTZ_JOB_DETAILS IS '任务详细信息表';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.sched_name IS '调度名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.job_name IS '任务名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.job_group IS '任务组名';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.description IS '相关介绍';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.job_class_name IS '执行任务类名称';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.is_durable IS '是否持久化';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.is_nonconcurrent IS '是否并发';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.is_update_data IS '是否更新数据';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.requests_recovery IS '是否接受恢复执行';
COMMENT ON COLUMN QRTZ_JOB_DETAILS.job_data IS '存放持久化job对象';

-- 2、存储已配置的 Trigger 的信息
CREATE TABLE QRTZ_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL,
    trigger_name         VARCHAR(200)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    job_name             VARCHAR(200)    NOT NULL,
    job_group            VARCHAR(200)    NOT NULL,
    description          VARCHAR(250)    NULL,
    next_fire_time       BIGINT          NULL,
    prev_fire_time       BIGINT          NULL,
    priority             INTEGER         NULL,
    trigger_state        VARCHAR(16)     NOT NULL,
    trigger_type         VARCHAR(8)      NOT NULL,
    start_time           BIGINT          NOT NULL,
    end_time             BIGINT          NULL,
    calendar_name        VARCHAR(200)    NULL,
    misfire_instr        SMALLINT        NULL,
    job_data             BYTEA           NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, job_name, job_group) REFERENCES QRTZ_JOB_DETAILS(sched_name, job_name, job_group)
);
COMMENT ON TABLE QRTZ_TRIGGERS IS '触发器信息表';
COMMENT ON COLUMN QRTZ_TRIGGERS.sched_name IS '调度名称';
COMMENT ON COLUMN QRTZ_TRIGGERS.trigger_name IS '触发器的名字';
COMMENT ON COLUMN QRTZ_TRIGGERS.trigger_group IS '触发器所属组的名字';
COMMENT ON COLUMN QRTZ_TRIGGERS.job_name IS 'qrtz_job_details表job_name的外键';
COMMENT ON COLUMN QRTZ_TRIGGERS.job_group IS 'qrtz_job_details表job_group的外键';
COMMENT ON COLUMN QRTZ_TRIGGERS.description IS '相关介绍';
COMMENT ON COLUMN QRTZ_TRIGGERS.next_fire_time IS '下一次触发时间（毫秒）';
COMMENT ON COLUMN QRTZ_TRIGGERS.prev_fire_time IS '上一次触发时间（毫秒）';
COMMENT ON COLUMN QRTZ_TRIGGERS.priority IS '优先级';
COMMENT ON COLUMN QRTZ_TRIGGERS.trigger_state IS '触发器状态';
COMMENT ON COLUMN QRTZ_TRIGGERS.trigger_type IS '触发器的类型';
COMMENT ON COLUMN QRTZ_TRIGGERS.start_time IS '开始时间';
COMMENT ON COLUMN QRTZ_TRIGGERS.end_time IS '结束时间';
COMMENT ON COLUMN QRTZ_TRIGGERS.calendar_name IS '日程表名称';
COMMENT ON COLUMN QRTZ_TRIGGERS.misfire_instr IS '补偿执行的策略';
COMMENT ON COLUMN QRTZ_TRIGGERS.job_data IS '存放持久化job对象';

-- 3、存储简单的 Trigger（重复次数、间隔等）
CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL,
    trigger_name         VARCHAR(200)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    repeat_count         BIGINT          NOT NULL,
    repeat_interval      BIGINT          NOT NULL,
    times_triggered      BIGINT          NOT NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE QRTZ_SIMPLE_TRIGGERS IS '简单触发器的信息表';
COMMENT ON COLUMN QRTZ_SIMPLE_TRIGGERS.repeat_count IS '重复次数';
COMMENT ON COLUMN QRTZ_SIMPLE_TRIGGERS.repeat_interval IS '重复间隔';
COMMENT ON COLUMN QRTZ_SIMPLE_TRIGGERS.times_triggered IS '已触发次数';

-- 4、存储 Cron Trigger
CREATE TABLE QRTZ_CRON_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL,
    trigger_name         VARCHAR(200)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    cron_expression      VARCHAR(200)    NOT NULL,
    time_zone_id         VARCHAR(80),
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE QRTZ_CRON_TRIGGERS IS 'Cron类型的触发器表';
COMMENT ON COLUMN QRTZ_CRON_TRIGGERS.cron_expression IS 'cron表达式';
COMMENT ON COLUMN QRTZ_CRON_TRIGGERS.time_zone_id IS '时区';

-- 5、Trigger 作为 Blob 类型存储
CREATE TABLE QRTZ_BLOB_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL,
    trigger_name         VARCHAR(200)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    blob_data            BYTEA           NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE QRTZ_BLOB_TRIGGERS IS 'Blob类型的触发器表';
COMMENT ON COLUMN QRTZ_BLOB_TRIGGERS.blob_data IS '存放持久化Trigger对象';

-- 6、存放 Calendar 信息
CREATE TABLE QRTZ_CALENDARS (
    sched_name           VARCHAR(120)    NOT NULL,
    calendar_name        VARCHAR(200)    NOT NULL,
    calendar             BYTEA           NOT NULL,
    PRIMARY KEY (sched_name, calendar_name)
);
COMMENT ON TABLE QRTZ_CALENDARS IS '日历信息表';
COMMENT ON COLUMN QRTZ_CALENDARS.calendar_name IS '日历名称';
COMMENT ON COLUMN QRTZ_CALENDARS.calendar IS '存放持久化calendar对象';

-- 7、暂停的触发器组
CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    sched_name           VARCHAR(120)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    PRIMARY KEY (sched_name, trigger_group)
);
COMMENT ON TABLE QRTZ_PAUSED_TRIGGER_GRPS IS '暂停的触发器表';

-- 8、已触发的 Trigger
CREATE TABLE QRTZ_FIRED_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL,
    entry_id             VARCHAR(95)     NOT NULL,
    trigger_name         VARCHAR(200)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    instance_name        VARCHAR(200)    NOT NULL,
    fired_time           BIGINT          NOT NULL,
    sched_time           BIGINT          NOT NULL,
    priority             INTEGER         NOT NULL,
    state                VARCHAR(16)     NOT NULL,
    job_name             VARCHAR(200)    NULL,
    job_group            VARCHAR(200)    NULL,
    is_nonconcurrent     VARCHAR(1)      NULL,
    requests_recovery    VARCHAR(1)      NULL,
    PRIMARY KEY (sched_name, entry_id)
);
COMMENT ON TABLE QRTZ_FIRED_TRIGGERS IS '已触发的触发器表';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.entry_id IS '调度器实例id';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.trigger_name IS '触发器名称';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.trigger_group IS '触发器组名';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.instance_name IS '实例名';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.fired_time IS '触发时间';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.sched_time IS '调度时间';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.priority IS '优先级';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.state IS '状态';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.job_name IS '任务名称';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.job_group IS '任务组名';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.is_nonconcurrent IS '是否并发';
COMMENT ON COLUMN QRTZ_FIRED_TRIGGERS.requests_recovery IS '是否接受恢复执行';

-- 9、调度器状态
CREATE TABLE QRTZ_SCHEDULER_STATE (
    sched_name           VARCHAR(120)    NOT NULL,
    instance_name        VARCHAR(200)    NOT NULL,
    last_checkin_time    BIGINT          NOT NULL,
    checkin_interval     BIGINT          NOT NULL,
    PRIMARY KEY (sched_name, instance_name)
);
COMMENT ON TABLE QRTZ_SCHEDULER_STATE IS '调度器状态表';
COMMENT ON COLUMN QRTZ_SCHEDULER_STATE.last_checkin_time IS '上次检查时间';
COMMENT ON COLUMN QRTZ_SCHEDULER_STATE.checkin_interval IS '检查间隔时间';

-- 10、锁
CREATE TABLE QRTZ_LOCKS (
    sched_name           VARCHAR(120)    NOT NULL,
    lock_name            VARCHAR(40)     NOT NULL,
    PRIMARY KEY (sched_name, lock_name)
);
COMMENT ON TABLE QRTZ_LOCKS IS '锁表';

-- Simprop triggers (Quartz 2.x 新增)
CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL,
    trigger_name         VARCHAR(200)    NOT NULL,
    trigger_group        VARCHAR(200)    NOT NULL,
    str_prop_1           VARCHAR(512)    NULL,
    str_prop_2           VARCHAR(512)    NULL,
    str_prop_3           VARCHAR(512)    NULL,
    int_prop_1           INT             NULL,
    int_prop_2           INT             NULL,
    long_prop_1          BIGINT          NULL,
    long_prop_2          BIGINT          NULL,
    dec_prop_1           NUMERIC(13,4)   NULL,
    dec_prop_2           NUMERIC(13,4)   NULL,
    bool_prop_1          BOOL            NULL,
    bool_prop_2          BOOL            NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
);
COMMENT ON TABLE QRTZ_SIMPROP_TRIGGERS IS 'SimProp触发器表';

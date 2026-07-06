-- v3.9.2 新增：公告已读记录表（补丁包 v3.9.1 中没有此表）
CREATE TABLE IF NOT EXISTS sys_notice_read (
  read_id   BIGSERIAL PRIMARY KEY,
  notice_id INT NOT NULL,
  user_id   BIGINT NOT NULL,
  read_time TIMESTAMP NOT NULL,
  CONSTRAINT uk_user_notice UNIQUE (user_id, notice_id)
);
COMMENT ON TABLE sys_notice_read IS '公告已读记录表';
COMMENT ON COLUMN sys_notice_read.read_id IS '已读主键';
COMMENT ON COLUMN sys_notice_read.notice_id IS '公告id';
COMMENT ON COLUMN sys_notice_read.user_id IS '用户id';
COMMENT ON COLUMN sys_notice_read.read_time IS '阅读时间';

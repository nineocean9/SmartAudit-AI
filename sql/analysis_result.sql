-- ============================================================
-- 分析结果表（AI 数据分析仪表盘）
-- ============================================================
CREATE TABLE IF NOT EXISTS analysis_result (
  id            BIGSERIAL       PRIMARY KEY,
  title         VARCHAR(255),
  chart_data    JSONB           NOT NULL,
  summary       TEXT,
  conversation_id BIGINT,
  create_by     VARCHAR(64)     DEFAULT '',
  create_time   TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE analysis_result IS 'AI 数据分析结果';
COMMENT ON COLUMN analysis_result.chart_data IS '图表数据 JSON (charts:[{type,title,labels,datasets}])';
COMMENT ON COLUMN analysis_result.summary IS '分析总结文字';
CREATE INDEX idx_ar_time ON analysis_result(create_time DESC);

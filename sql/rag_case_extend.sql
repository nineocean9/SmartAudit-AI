-- RAG 知识库扩展：为 document_chunk 增加 source_type 字段，支持案例库和风险案例库向量检索
-- 执行时间：2026-07-10

-- 1. 为 document_chunk 表增加 source_type 字段，区分不同来源
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'PROJECT';
COMMENT ON COLUMN document_chunk.source_type IS '来源类型: PROJECT-项目文档, CASE-案例库, RISK_CASE-风险案例库';

-- 2. 为 document_chunk 表增加 source_id 字段，关联原始记录ID
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS source_id BIGINT;
COMMENT ON COLUMN document_chunk.source_id IS '来源记录ID: 案例库id/风险案例id（source_type非PROJECT时使用）';

-- 3. 为 source_type 建立索引，加速按类型检索
CREATE INDEX IF NOT EXISTS idx_dc_source_type ON document_chunk(source_type);
CREATE INDEX IF NOT EXISTS idx_dc_source ON document_chunk(source_type, source_id);

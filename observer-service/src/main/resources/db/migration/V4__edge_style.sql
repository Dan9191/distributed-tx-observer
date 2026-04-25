-- Стиль линии ребра на канвасе шаблона транзакции.
-- Возможные значения: default (bezier), straight, smoothstep, dashed, dotted.
ALTER TABLE step_edge ADD COLUMN style VARCHAR(50) NOT NULL DEFAULT 'default';

COMMENT ON COLUMN step_edge.style IS 'Стиль линии ребра: default | straight | smoothstep | dashed | dotted';

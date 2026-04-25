-- Добавляем поддержку маркеров начала и конца транзакции на канвасе.

ALTER TABLE step_template
    ADD COLUMN node_type VARCHAR(20) NOT NULL DEFAULT 'step';

ALTER TABLE step_template
    ADD COLUMN transaction_name VARCHAR(255) NULL;

ALTER TABLE step_template
    ALTER COLUMN step_id DROP NOT NULL;

UPDATE step_template st
SET transaction_name = sd.transaction_name
FROM step_definition sd
WHERE st.step_id = sd.id;

COMMENT ON COLUMN step_template.node_type        IS 'Тип узла канваса: step | start | end';
COMMENT ON COLUMN step_template.transaction_name IS 'Название транзакции (денормализация для маркеров и запросов)';

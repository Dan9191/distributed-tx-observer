-- Переключаем рёбра с уровня определений шагов на уровень экземпляров шаблона.
-- Это позволяет размещать несколько экземпляров одного шага на канвасе
-- и соединять их независимыми рёбрами.

-- Удаляем старые FK (ссылались на step_definition)
ALTER TABLE step_edge DROP CONSTRAINT step_edge_from_step_id_fkey;
ALTER TABLE step_edge DROP CONSTRAINT step_edge_to_step_id_fkey;

-- Очищаем существующие рёбра: старые ID ссылались на step_definition,
-- новые будут ссылаться на step_template — данные несовместимы.
TRUNCATE TABLE step_edge;

-- Переименовываем колонки
ALTER TABLE step_edge RENAME COLUMN from_step_id TO from_instance_id;
ALTER TABLE step_edge RENAME COLUMN to_step_id   TO to_instance_id;

-- Добавляем новые FK с каскадным удалением:
-- при удалении экземпляра шаблона связанные рёбра удаляются автоматически.
ALTER TABLE step_edge
    ADD CONSTRAINT step_edge_from_instance_id_fkey
        FOREIGN KEY (from_instance_id) REFERENCES step_template(id) ON DELETE CASCADE;
ALTER TABLE step_edge
    ADD CONSTRAINT step_edge_to_instance_id_fkey
        FOREIGN KEY (to_instance_id) REFERENCES step_template(id) ON DELETE CASCADE;

COMMENT ON COLUMN step_edge.from_instance_id IS 'Экземпляр шага на канвасе (откуда идёт стрелка)';
COMMENT ON COLUMN step_edge.to_instance_id   IS 'Экземпляр шага на канвасе (куда идёт стрелка)';

COMMENT ON TABLE step_template IS 'Экземпляры шагов на канвасе шаблона; один шаг может иметь несколько экземпляров';

-- Реестр известных распределённых транзакций.
-- Запись создаётся автоматически при регистрации первого шага транзакции.
CREATE TABLE transaction_definition (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

COMMENT ON TABLE  transaction_definition      IS 'Реестр известных распределённых транзакций';
COMMENT ON COLUMN transaction_definition.id   IS 'Суррогатный ключ';
COMMENT ON COLUMN transaction_definition.name IS 'Уникальное название транзакции, например CreateOrder';


-- Реестр шагов: атомарных действий в рамках транзакции.
-- Регистрируется микросервисами при старте через observer-library.
-- Уникален по паре (transaction_name, step_name).
CREATE TABLE step_definition (
    id               BIGSERIAL PRIMARY KEY,
    transaction_name VARCHAR(255) NOT NULL,
    step_name        VARCHAR(255) NOT NULL,
    service_name     VARCHAR(255) NOT NULL,
    UNIQUE (transaction_name, step_name)
);

COMMENT ON TABLE  step_definition                  IS 'Реестр шагов транзакций, зарегистрированных микросервисами';
COMMENT ON COLUMN step_definition.id               IS 'Суррогатный ключ';
COMMENT ON COLUMN step_definition.transaction_name IS 'Название транзакции, которой принадлежит шаг';
COMMENT ON COLUMN step_definition.step_name        IS 'Название шага внутри транзакции';
COMMENT ON COLUMN step_definition.service_name     IS 'Микросервис, ответственный за выполнение шага';


-- Визуальные позиции шагов на канвасе шаблона транзакции.
-- Один шаг может присутствовать в шаблоне не более одного раза.
CREATE TABLE step_template (
    id      BIGSERIAL PRIMARY KEY,
    step_id BIGINT NOT NULL REFERENCES step_definition(id),
    pos_x   DOUBLE PRECISION NOT NULL DEFAULT 0,
    pos_y   DOUBLE PRECISION NOT NULL DEFAULT 0
);

COMMENT ON TABLE  step_template        IS 'Позиции шагов на канвасе шаблона транзакции';
COMMENT ON COLUMN step_template.id      IS 'Суррогатный ключ';
COMMENT ON COLUMN step_template.step_id IS 'Ссылка на шаг из реестра';
COMMENT ON COLUMN step_template.pos_x   IS 'Координата X на канвасе (пиксели)';
COMMENT ON COLUMN step_template.pos_y   IS 'Координата Y на канвасе (пиксели)';


-- Направленные рёбра между шагами на канвасе шаблона.
-- Определяют граф выполнения транзакции.
CREATE TABLE step_edge (
    id           BIGSERIAL PRIMARY KEY,
    from_step_id BIGINT NOT NULL REFERENCES step_definition(id),
    to_step_id   BIGINT NOT NULL REFERENCES step_definition(id)
);

COMMENT ON TABLE  step_edge              IS 'Направленные рёбра графа шаблона транзакции';
COMMENT ON COLUMN step_edge.id           IS 'Суррогатный ключ';
COMMENT ON COLUMN step_edge.from_step_id IS 'Исходящий шаг (откуда идёт стрелка)';
COMMENT ON COLUMN step_edge.to_step_id   IS 'Входящий шаг (куда идёт стрелка)';

-- Визуальные группы на канвасе шаблона транзакции.
-- Позволяют выделять области и подписывать части распределённой транзакции.
CREATE TABLE template_group (
    id               BIGSERIAL PRIMARY KEY,
    transaction_name VARCHAR(255)      NOT NULL,
    label            VARCHAR(255)      NOT NULL DEFAULT '',
    color            VARCHAR(50)       NOT NULL DEFAULT '#6366f1',
    pos_x            DOUBLE PRECISION  NOT NULL DEFAULT 0,
    pos_y            DOUBLE PRECISION  NOT NULL DEFAULT 0,
    width            DOUBLE PRECISION  NOT NULL DEFAULT 200,
    height           DOUBLE PRECISION  NOT NULL DEFAULT 150
);

COMMENT ON TABLE  template_group                  IS 'Визуальные группы (выделенные области) на канвасе шаблона';
COMMENT ON COLUMN template_group.id               IS 'Суррогатный ключ';
COMMENT ON COLUMN template_group.transaction_name IS 'Название транзакции';
COMMENT ON COLUMN template_group.label            IS 'Подпись группы';
COMMENT ON COLUMN template_group.color            IS 'Цвет группы (hex)';
COMMENT ON COLUMN template_group.pos_x            IS 'Координата X на канвасе';
COMMENT ON COLUMN template_group.pos_y            IS 'Координата Y на канвасе';
COMMENT ON COLUMN template_group.width            IS 'Ширина области';
COMMENT ON COLUMN template_group.height           IS 'Высота области';

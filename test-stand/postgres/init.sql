-- Создаём схему observer, в которой Flyway развернёт таблицы.
-- Скрипт выполняется один раз при первом запуске контейнера.
CREATE SCHEMA IF NOT EXISTS observer;
GRANT ALL ON SCHEMA observer TO rag_user;

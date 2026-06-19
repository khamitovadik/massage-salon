-- V2: Таблица пользователей

CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)  NOT NULL,
    phone            VARCHAR(20)   UNIQUE,
    email            VARCHAR(100)  UNIQUE,
    password         VARCHAR(255)  NOT NULL,
    role             VARCHAR(20)   NOT NULL DEFAULT 'CLIENT',
    telegram_chat_id BIGINT,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

-- Первый аккаунт руководителя (пароль: admin123)
INSERT INTO users (name, phone, email, password, role)
VALUES (
    'Администратор',
    '+70000000000',
    'admin@salon.ru',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'OWNER'
);

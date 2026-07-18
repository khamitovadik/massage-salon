-- Добавить колонку subscription_id в таблицу appointments
ALTER TABLE appointments ADD COLUMN subscription_id BIGINT;

-- Добавить внешний ключ на таблицу subscriptions
ALTER TABLE appointments
ADD CONSTRAINT fk_appointment_subscription
FOREIGN KEY (subscription_id) REFERENCES subscriptions(id);

-- Добавить индекс для быстрого поиска
CREATE INDEX idx_appointment_subscription_id ON appointments(subscription_id);

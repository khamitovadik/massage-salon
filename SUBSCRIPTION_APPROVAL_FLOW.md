# 📋 Процесс подтверждения абонемента и записей

## 🆕 Что было добавлено

✅ **Двухуровневое подтверждение:**
1. Абонемент создается в статусе **PENDING** (ждёт одобрения админа)
2. Запись тоже в **PENDING** (ждёт одобрения админа)
3. **При подтверждении запись → автоматически списывается сеанс** из абонемента

✅ **Связь запись ↔ абонемент:**
- Запись может использовать абонемент через новое поле `subscriptionId`
- При подтверждении запись сохраняет информацию об абонементе

---

## 📊 НОВЫЕ ENDPOINTS

### 1️⃣ Подтверждение абонемента

**Админ видит ОЖИДАЮЩИЕ абонементы:**
```bash
GET /api/subscriptions/pending
Authorization: Bearer ADMIN_TOKEN
```

**Ответ:**
```json
[
  {
    "id": 1,
    "clientId": 5,
    "clientName": "Адилет",
    "serviceId": 2,
    "serviceName": "Массаж спины",
    "totalSessions": 10,
    "remainingSessions": 10,
    "startDate": "2026-08-01",
    "expiryDate": "2026-12-31",
    "status": "PENDING",
    "notes": "куплен по акции",
    "createdAt": "2026-07-18T09:00:00"
  }
]
```

**✅ Админ одобряет абонемент:**
```bash
PATCH /api/subscriptions/1/approve
Authorization: Bearer ADMIN_TOKEN
```

**❌ Админ отклоняет абонемент (с причиной):**
```bash
PATCH /api/subscriptions/1/reject?reason=Превышен лимит абонементов
Authorization: Bearer ADMIN_TOKEN
```

---

## 🔄 ПОЛНЫЙ ПРОЦЕСС

### Шаг 1️⃣: Клиент запрашивает абонемент

```bash
POST /api/subscriptions
Authorization: Bearer CUSTOMER_TOKEN
Content-Type: application/json

{
  "serviceId": 2,
  "totalSessions": 10,
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "notes": "куплен по акции"
}
```

**Ответ:**
```json
{
  "id": 1,
  "clientId": 5,
  "clientName": "Адилет",
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "totalSessions": 10,
  "remainingSessions": 10,
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "status": "PENDING",    ← ⏳ ОЖИДАЕТ ПОДТВЕРЖДЕНИЯ
  "notes": "куплен по акции",
  "createdAt": "2026-07-18T09:00:00"
}
```

---

### Шаг 2️⃣: Админ подтверждает абонемент

```bash
PATCH /api/subscriptions/1/approve
Authorization: Bearer ADMIN_TOKEN
```

**Ответ:**
```json
{
  "id": 1,
  "clientId": 5,
  "clientName": "Адилет",
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "totalSessions": 10,
  "remainingSessions": 10,
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "status": "ACTIVE",     ← ✅ ОДОБРЕН И АКТИВЕН
  "notes": "куплен по акции",
  "createdAt": "2026-07-18T09:00:00"
}
```

---

### Шаг 3️⃣: Клиент создает запись ИСПОЛЬЗУЯ абонемент

```bash
POST /api/appointments
Authorization: Bearer CUSTOMER_TOKEN
Content-Type: application/json

{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00",
  "subscriptionId": 1      ← ✅ НОВОЕ: указываем какой абонемент использовать
}
```

**Логика проверки:**
- ✅ Абонемент должен быть ACTIVE
- ✅ Услуга в абонементе должна совпадать со услугой в записи
- ✅ Срок действия абонемента не должен быть истекшим
- ✅ Должны быть доступные сеансы (remainingSessions > 0)

**Ответ:**
```json
{
  "id": 123,
  "clientId": 5,
  "clientName": "Адилет",
  "clientPhone": "+77072302002",
  "employeeId": 1,
  "employeeName": "Айгуль",
  "employeeSpecialization": "Классический массаж",
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "servicePrice": 5000,
  "durationMinutes": 60,
  "startTime": "2026-08-15T10:00:00",
  "endTime": "2026-08-15T11:00:00",
  "status": "PENDING",     ← ⏳ ОЖИДАЕТ ПОДТВЕРЖДЕНИЯ
  "comment": null,
  "createdAt": "2026-07-18T14:30:00",
  "subscriptionId": 1,     ← ✅ НОВОЕ: информация об абонементе
  "subscriptionStatus": "ACTIVE"
}
```

---

### Шаг 4️⃣: Админ подтверждает запись

```bash
PATCH /api/appointments/123/status?status=CONFIRMED
Authorization: Bearer ADMIN_TOKEN
```

**ЧТО ПРОИСХОДИТ АВТОМАТИЧЕСКИ:**
1. Статус записи меняется на CONFIRMED ✅
2. **Проверяется абонемент** - есть ли он и ACTIVE ли
3. **Списывается 1 сеанс** из абонемента 📉
4. Если это был последний сеанс → статус абонемента меняется на EXHAUSTED

**Ответ:**
```json
{
  "id": 123,
  "status": "CONFIRMED",   ← ✅ ПОДТВЕРЖДЕНА
  "subscriptionId": 1,
  "subscriptionStatus": "ACTIVE",
  ...
}
```

---

### Шаг 5️⃣: Клиент видит что осталось 9 сеансов

```bash
GET /api/subscriptions/1
Authorization: Bearer CUSTOMER_TOKEN
```

**Ответ:**
```json
{
  "id": 1,
  "clientId": 5,
  "clientName": "Адилет",
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "totalSessions": 10,
  "remainingSessions": 9,     ← ⬇️ УМЕНЬШИЛОСЬ НА 1
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "status": "ACTIVE",
  "notes": "куплен по акции",
  "createdAt": "2026-07-18T09:00:00"
}
```

---

## 📱 СЦЕНАРИИ

### Сценарий А: Запись без абонемента (просто платная запись)

```bash
# 1. Клиент создает запись БЕЗ абонемента
POST /api/appointments
{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00"
  // subscriptionId НЕ указан
}

# 2. Админ подтверждает
PATCH /api/appointments/123/status?status=CONFIRMED

# 3. Абонемент НЕ менялся (записались без абонемента)
```

---

### Сценарий Б: Запись с абонементом (рекомендуется)

```bash
# 1. Клиент запрашивает абонемент
POST /api/subscriptions
{
  "serviceId": 2,
  "totalSessions": 10,
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31"
}
→ Создается с status: PENDING

# 2. Админ подтверждает абонемент
PATCH /api/subscriptions/1/approve
→ Меняется на status: ACTIVE

# 3. Клиент создает запись используя абонемент
POST /api/appointments
{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00",
  "subscriptionId": 1
}
→ Создается запись с subscriptionId=1, status=PENDING

# 4. Админ подтверждает запись
PATCH /api/appointments/123/status?status=CONFIRMED
→ Запись становится CONFIRMED
→ АВТОМАТИЧЕСКИ списывается 1 сеанс из абонемента
→ remainingSessions: 10 → 9

# 5. Повторяется: клиент может создать ещё записей используя оставшиеся 9 сеансов
```

---

### Сценарий В: Отклонение абонемента

```bash
# 1. Клиент запрашивает
POST /api/subscriptions → status: PENDING

# 2. Админ отклоняет
PATCH /api/subscriptions/1/reject?reason=Проблема с платежом

# 3. Абонемент становится CANCELLED
# 4. Клиент видит что абонемент отклонен
```

---

## 🔒 ПРАВА ДОСТУПА

| Действие | CLIENT | ADMIN/OWNER |
|----------|--------|------------|
| Создать абонемент (на себя) | ✅ | ✅ (может на клиента) |
| Видеть свой абонемент | ✅ | ❌ |
| Видеть ОЖИДАЮЩИЕ (/pending) | ❌ | ✅ |
| Одобрить абонемент | ❌ | ✅ |
| Отклонить абонемент | ❌ | ✅ |
| Создать запись (на себя) | ✅ | ✅ (может на клиента) |
| Использовать абонемент | ✅ (свой) | ✅ (любой) |
| Подтвердить запись | ❌ | ✅ |

---

## 🐛 ОБРАБОТКА ОШИБОК

### Если клиент попытается создать запись с неправильным абонементом:

```bash
POST /api/appointments
{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00",
  "subscriptionId": 999  ← абонемента не существует
}
```

**Ответ:**
```json
{
  "message": "Абонемент не найден: 999"
}
```

---

### Если статус абонемента не ACTIVE:

```bash
POST /api/appointments
{
  "subscriptionId": 1  ← status: PENDING (не одобрен еще)
}
```

**Ответ:**
```json
{
  "message": "Абонемент не активен. Статус: PENDING"
}
```

---

### Если нет доступных сеансов:

```bash
POST /api/appointments
{
  "subscriptionId": 1  ← remainingSessions: 0
}
```

**Ответ:**
```json
{
  "message": "Нет доступных сеансов в абонементе"
}
```

---

## 💾 СТРУКТУРА БД

### Таблица subscriptions

```sql
CREATE TABLE subscriptions (
    id BIGINT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    total_sessions INTEGER NOT NULL,
    remaining_sessions INTEGER NOT NULL,
    start_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  ← НОВОЕ: PENDING по умолчанию
    notes TEXT,
    created_at TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES users(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);
```

### Таблица appointments

```sql
ALTER TABLE appointments ADD COLUMN subscription_id BIGINT;
ALTER TABLE appointments ADD FOREIGN KEY (subscription_id) REFERENCES subscriptions(id);
-- Скрипт: V3__add_subscription_to_appointments.sql
```

---

## ✅ ТЕСТИРОВАНИЕ

```bash
# 1. Создать абонемент
curl -X POST http://localhost:8081/api/subscriptions \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": 2,
    "totalSessions": 10,
    "startDate": "2026-08-01",
    "expiryDate": "2026-12-31"
  }'
# → Ответ: id=1, status=PENDING

# 2. Админ видит ОЖИДАЮЩИЕ
curl -X GET http://localhost:8081/api/subscriptions/pending \
  -H "Authorization: Bearer ADMIN_TOKEN"
# → Ответ: массив с нашим абонементом

# 3. Админ одобряет
curl -X PATCH http://localhost:8081/api/subscriptions/1/approve \
  -H "Authorization: Bearer ADMIN_TOKEN"
# → Ответ: status=ACTIVE

# 4. Клиент создает запись с абонементом
curl -X POST http://localhost:8081/api/appointments \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "serviceId": 2,
    "startTime": "2026-08-15T10:00:00",
    "subscriptionId": 1
  }'
# → Ответ: id=123, status=PENDING, subscriptionId=1

# 5. Админ подтверждает запись
curl -X PATCH http://localhost:8081/api/appointments/123/status?status=CONFIRMED \
  -H "Authorization: Bearer ADMIN_TOKEN"
# → Ответ: status=CONFIRMED

# 6. Проверить что сеанс списан
curl -X GET http://localhost:8081/api/subscriptions/1 \
  -H "Authorization: Bearer CUSTOMER_TOKEN"
# → Ответ: remainingSessions=9 (было 10)
```

---

## 🎯 ЧТО ПОЛУЧИЛОСЬ

| Что | Было | Теперь |
|-----|------|--------|
| Абонемент требует подтверждения | ❌ | ✅ Да (PENDING) |
| Запись связана с абонементом | ❌ | ✅ Да (subscriptionId) |
| Автоматическое списание | ❌ | ✅ При CONFIRMED |
| Информация об абонементе в ответе | ❌ | ✅ subscriptionId, subscriptionStatus |
| Админ видит ОЖИДАЮЩИЕ абонементы | ❌ | ✅ /pending endpoint |
| Админ может одобрить/отклонить | ❌ | ✅ /approve, /reject endpoints |

---

## 📝 ФАЙЛЫ КОТОРЫЕ БЫЛИ ИЗМЕНЕНЫ

✅ `SubscriptionStatus.java` - добавлен PENDING
✅ `Appointment.java` - добавлено поле subscription
✅ `SubscriptionService.java` - методы approve(), reject(), getByStatus()
✅ `SubscriptionController.java` - endpoints /pending, /approve, /reject
✅ `CreateAppointmentRequest.java` - добавлено subscriptionId
✅ `AppointmentService.java` - проверка абонемента, автоматическое списание
✅ `AppointmentResponse.java` - добавлены subscriptionId, subscriptionStatus
✅ `SubscriptionRepository.java` - метод findAllByStatusOrderByCreatedAtDesc
✅ `V3__add_subscription_to_appointments.sql` - миграция БД

---

## 🚀 ГОТОВО К ИСПОЛЬЗОВАНИЮ!

Все компоненты интегрированы и протестированы. Теперь можно:
- ✅ Клиент запрашивает абонемент (PENDING)
- ✅ Админ видит запросы и одобряет (ACTIVE)
- ✅ Клиент создает записи используя абонемент
- ✅ Админ подтверждает записи
- ✅ Сеансы автоматически списываются
- ✅ Фронтенд видит информацию об абонементе в записи

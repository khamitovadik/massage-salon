# Записи и Абонементы — Концепция и реализация

## Общая концепция

Две главные сущности системы:
- **Запись (Appointment)** — конкретный визит клиента к сотруднику в определённое время
- **Абонемент (Subscription)** — предоплаченный пакет сеансов одной услуги

Они независимы друг от друга. Абонемент пока не списывается автоматически при создании записи — это **нужно доработать**.

---

## ЗАПИСИ (Appointments)

### Жизненный цикл записи

```
PENDING → CONFIRMED → COMPLETED
                ↘
             CANCELLED (из любого статуса кроме COMPLETED)
```

| Статус | Кто ставит | Описание |
|--------|-----------|----------|
| PENDING | Система автоматически | При создании записи |
| CONFIRMED | ADMIN / OWNER | Подтвердили визит |
| COMPLETED | ADMIN / OWNER | Сеанс прошёл |
| CANCELLED | Клиент (свою) / ADMIN / OWNER | Отменена |

### Структура записи (таблица `appointments`)

```java
Appointment {
    id              Long
    client          User        // кто записался
    employee        Employee    // к кому записались
    service         SalonService // какая услуга
    startTime       LocalDateTime
    endTime         LocalDateTime  // = startTime + service.durationMinutes
    status          AppointmentStatus
    comment         String       // комментарий клиента (необязательно)
    createdAt       LocalDateTime
}
```

### Создание записи — логика

```
POST /api/appointments
Body: {
  employeeId: 1,
  serviceId: 2,
  startTime: "2026-08-01T10:00:00",
  comment: "прошу без масла",
  clientId: null  // null = записывается сам, число = ADMIN записывает другого клиента
}
```

**Что происходит внутри:**
1. Определяем клиента (из токена или по clientId если ADMIN)
2. Проверяем что сотрудник активен
3. Проверяем что услуга активна
4. `endTime = startTime + service.durationMinutes`
5. **Проверка конфликта** — нет ли у сотрудника другой записи в это время:
   ```sql
   SELECT COUNT > 0 WHERE employee_id = ? 
   AND status NOT IN ('CANCELLED')
   AND startTime < endTime AND endTime > startTime
   ```
6. Создаём запись со статусом PENDING
7. Отправляем Telegram уведомление сотруднику

### API записей

```
GET    /api/appointments              # все записи (ADMIN/OWNER)
GET    /api/appointments/my           # свои записи (любой авторизованный)
GET    /api/appointments/{id}         # одна запись
GET    /api/appointments/employee/{id} # записи конкретного сотрудника

POST   /api/appointments              # создать запись

PATCH  /api/appointments/{id}/status?status=CONFIRMED   # сменить статус (ADMIN/OWNER)
PATCH  /api/appointments/{id}/cancel  # отменить (клиент свою, ADMIN любую)

GET    /api/appointments/graph/all?from=...&to=...         # для графика (ADMIN/OWNER)
GET    /api/appointments/graph/employee/{id}?from=...&to=... # для графика по сотруднику
```

### Ответ API (AppointmentResponse)

```json
{
  "id": 5,
  "clientId": 3,
  "clientName": "Адилет",
  "clientPhone": "+77072302002",
  "employeeId": 1,
  "employeeName": "Айгуль Сейткали",
  "employeeSpecialization": "Классический массаж",
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "servicePrice": 5000,
  "durationMinutes": 60,
  "startTime": "2026-08-01T10:00:00",
  "endTime": "2026-08-01T11:00:00",
  "status": "PENDING",
  "comment": "прошу без масла",
  "createdAt": "2026-07-18T09:30:00"
}
```

### Telegram уведомления по записям

- При создании → сотрудник получает уведомление в Telegram
- Напоминание за 24 часа → клиенту (если привязан Telegram)
- `ReminderScheduler` запускается каждый час, ищет записи за следующие 24-25 часов

---

## АБОНЕМЕНТЫ (Subscriptions)

### Концепция

Клиент покупает пакет сеансов одной конкретной услуги с датой начала и конца.  
Например: **«10 сеансов массажа спины до 31 декабря 2026»**

При каждом посещении можно списать 1 сеанс (`useSession`).  
**Важно:** списание сейчас происходит вручную — нужно доработать автоматическое списание при завершении записи.

### Структура абонемента (таблица `subscriptions`)

```java
Subscription {
    id                Long
    client            User
    service           SalonService   // на какую услугу
    totalSessions     Integer        // куплено сеансов (напр. 10)
    remainingSessions Integer        // осталось сеансов
    startDate         LocalDate
    expiryDate        LocalDate      // срок действия
    status            SubscriptionStatus
    notes             String         // примечания
    createdAt         LocalDateTime
}
```

### Статусы абонемента

```
ACTIVE → EXHAUSTED    (когда remainingSessions = 0)
ACTIVE → EXPIRED      (когда expiryDate < today)
ACTIVE → CANCELLED    (ручная отмена)
```

| Статус | Описание |
|--------|---------|
| ACTIVE | Активен, можно использовать |
| EXHAUSTED | Все сеансы использованы |
| EXPIRED | Истёк срок действия |
| CANCELLED | Отменён администратором |

### API абонементов

```
GET  /api/subscriptions/my           # мои абонементы
GET  /api/subscriptions/my/active    # только активные
GET  /api/subscriptions              # все (ADMIN/OWNER)
GET  /api/subscriptions/client/{id}  # абонементы клиента (ADMIN/OWNER)
GET  /api/subscriptions/{id}

POST /api/subscriptions              # создать (ADMIN/OWNER)
POST /api/subscriptions/{id}/use     # списать 1 сеанс

PUT  /api/subscriptions/{id}/cancel  # отменить (ADMIN/OWNER)
```

### Создание абонемента

```
POST /api/subscriptions
Body: {
  clientId: 3,          // обязательно для ADMIN
  serviceId: 2,
  totalSessions: 10,
  startDate: "2026-08-01",
  expiryDate: "2026-12-31",
  notes: "куплен по акции"
}
```

### Списание сеанса — логика

```
POST /api/subscriptions/{id}/use
```

1. Проверяем что абонемент принадлежит клиенту (или запрос от ADMIN)
2. Проверяем статус = ACTIVE
3. Проверяем что `expiryDate >= today` (иначе → EXPIRED)
4. Проверяем `remainingSessions > 0` (иначе → EXHAUSTED)
5. `remainingSessions -= 1`
6. Если `remainingSessions == 0` → статус EXHAUSTED

### Ответ API (SubscriptionResponse)

```json
{
  "id": 1,
  "clientId": 3,
  "clientName": "Адилет",
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "totalSessions": 10,
  "remainingSessions": 7,
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "status": "ACTIVE",
  "notes": "куплен по акции",
  "createdAt": "2026-07-18T09:00:00"
}
```

---

## Связь между записями и абонементами

**Сейчас:** никак не связаны автоматически.

**Что нужно доработать:**

### Вариант 1 — При создании записи (рекомендуется)
В `CreateAppointmentRequest` добавить поле `subscriptionId`.  
В `AppointmentService.create()` добавить:
```java
if (req.getSubscriptionId() != null) {
    subscriptionService.useSession(req.getSubscriptionId(), currentUserEmail);
}
```

### Вариант 2 — При завершении записи
Когда ADMIN меняет статус на COMPLETED, автоматически списывать сеанс с активного абонемента клиента на эту услугу.

```java
// В AppointmentService.updateStatus():
if (newStatus == COMPLETED) {
    // найти активный абонемент клиента на эту услугу
    subscriptionRepository
      .findActiveByClientAndService(appointment.getClient(), appointment.getService())
      .ifPresent(sub -> subscriptionService.useSession(sub.getId(), ...));
}
```

---

## Что нужно доработать в записях ❌

### Критично
- [ ] **Публичная онлайн-запись** — страница без логина:  
  Клиент выбирает: Услуга → Сотрудник → Дата → Время → Имя/Телефон → Готово  
  Бэкенд: `POST /api/appointments/public` (без токена, принимает имя+телефон)  
  Автоматически создаёт/находит User по телефону

- [ ] **Связь абонемент ↔ запись** — при создании записи предлагать использовать абонемент

- [ ] **Выбор свободного времени** — сейчас фронтенд принимает любую дату/время. Нужен виджет выбора времени который показывает только свободные слоты (`GET /api/schedule/slots?employeeId=&serviceId=&date=`)

### Важно
- [ ] **Фильтрация записей** — по статусу, дате, сотруднику, клиенту
- [ ] **Калькуляр/визуальный календарь** — отображение записей по дням/неделям
- [ ] **Повторная запись** — кнопка "Записаться снова" на завершённой записи
- [ ] **Email подтверждение** — при создании записи отправлять email клиенту

### Мелкое
- [ ] **Пагинация** — сейчас все записи грузятся сразу
- [ ] **Поиск клиента** по имени/телефону при создании записи (для ADMIN)
- [ ] **Цена из абонемента** — если используется абонемент, цена = 0

---

## Что нужно доработать в абонементах ❌

- [ ] **Автосписание** при завершении записи (см. выше)
- [ ] **Показывать абонемент** на странице создания записи если у клиента есть активный на нужную услугу
- [ ] **Автообновление статуса** — `expireOutdated()` метод есть, но не вызывается по расписанию. Добавить `@Scheduled`:
  ```java
  @Scheduled(cron = "0 0 1 * * *")  // каждый день в 01:00
  public void expireSubscriptions() {
      subscriptionService.expireOutdated();
  }
  ```
- [ ] **Продление абонемента** — endpoint `PUT /api/subscriptions/{id}/extend`
- [ ] **Уведомление** когда остался 1 сеанс или истекает срок

---

## Файлы реализации

### Бэкенд
```
entity/Appointment.java
entity/AppointmentStatus.java     // PENDING, CONFIRMED, COMPLETED, CANCELLED
entity/Subscription.java
entity/SubscriptionStatus.java    // ACTIVE, EXHAUSTED, EXPIRED, CANCELLED

service/AppointmentService.java   // создание, отмена, статусы, аналитика
service/SubscriptionService.java  // создание, списание, истечение

controller/AppointmentController.java
controller/SubscriptionController.java

repository/AppointmentRepository.java  // сложные JPQL запросы для аналитики
repository/SubscriptionRepository.java

dto/request/CreateAppointmentRequest.java
dto/request/CreateSubscriptionRequest.java
dto/response/AppointmentResponse.java
dto/response/SubscriptionResponse.java
```

### Фронтенд
```
pages/AppointmentsPage.tsx      // список записей + кнопки статусов
pages/NewAppointmentPage.tsx    // форма создания записи
pages/SubscriptionsPage.tsx     // список абонементов

api/appointments.ts
api/subscriptions.ts
```

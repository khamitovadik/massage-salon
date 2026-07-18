# 🔧 Реальная реализация: Записи и Абонементы

## 📌 Краткое резюме

**Записи (Appointments)** и **Абонементы (Subscriptions)** — это **независимые** системы. Они не связаны автоматически.

- ✅ Можно создать **Запись БЕЗ Абонемента** — просто запись на услугу
- ✅ Можно создать **Абонемент БЕЗ Записи** — просто купить пакет сеансов
- ❌ **Не реализовано:** Автоматическое списание сеансов при создании/завершении записи

---

## 📋 СУЩНОСТИ (Entity)

### 1. Appointment (Запись)

**Таблица:** `appointments`

```java
@Entity
public class Appointment {
    Long id;
    User client;              // кто записался
    Employee employee;        // к кому записались
    SalonService service;     // какая услуга
    LocalDateTime startTime;  // когда начинается
    LocalDateTime endTime;    // когда кончается (вычисляется)
    AppointmentStatus status; // PENDING, CONFIRMED, COMPLETED, CANCELLED
    String comment;           // комментарий клиента
    LocalDateTime createdAt;  // когда создана
}
```

**Статусы:**
```
PENDING (создана) 
    ↓
CONFIRMED (админ подтвердил)
    ↓
COMPLETED (сеанс прошёл)

или CANCELLED из любого состояния
```

---

### 2. Subscription (Абонемент)

**Таблица:** `subscriptions`

```java
@Entity
public class Subscription {
    Long id;
    User client;              // кому принадлежит
    SalonService service;     // на какую услугу
    Integer totalSessions;    // всего куплено сеансов (напр. 10)
    Integer remainingSessions;// осталось сеансов
    LocalDate startDate;      // когда начинает действовать
    LocalDate expiryDate;     // когда кончает действовать
    SubscriptionStatus status;// ACTIVE, EXHAUSTED, EXPIRED, CANCELLED
    String notes;             // примечания
    LocalDateTime createdAt;  // когда куплен
}
```

**Статусы:**
```
ACTIVE (активен)
    ↓
EXHAUSTED (все сеансы использованы)
или EXPIRED (срок действия истёк)
или CANCELLED (отменён вручную)
```

---

## 🎯 API ENDPOINTS

### Записи (Appointments)

```
POST /api/appointments                          Создать запись
GET  /api/appointments/my                       Мои записи
GET  /api/appointments                          Все записи (ADMIN/OWNER)
GET  /api/appointments/employee/{id}            Записи сотрудника
GET  /api/appointments/{id}                     Одна запись
PATCH /api/appointments/{id}/status?status=...  Изменить статус (ADMIN/OWNER)
PATCH /api/appointments/{id}/cancel             Отменить запись
GET  /api/appointments/graph/all?from=...&to=... Записи за период (для графика)
GET  /api/appointments/graph/employee/{id}?...  Записи сотрудника за период
```

### Абонементы (Subscriptions)

```
POST /api/subscriptions                         Создать абонемент
GET  /api/subscriptions/my                      Мои абонементы
GET  /api/subscriptions/my/active               Мои активные абонементы
GET  /api/subscriptions                         Все абонементы (ADMIN/OWNER)
GET  /api/subscriptions/client/{id}             Абонементы клиента (ADMIN/OWNER)
GET  /api/subscriptions/{id}                    Один абонемент
PATCH /api/subscriptions/{id}/use               Списать 1 сеанс
PATCH /api/subscriptions/{id}/cancel            Отменить абонемент (ADMIN/OWNER)
```

---

## 🔄 ЖИЗНЕННЫЙ ЦИКЛ ЗАПИСИ

### Шаг 1: Создание записи

```http
POST /api/appointments
Content-Type: application/json
Authorization: Bearer JWT_TOKEN

{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00",
  "comment": "прошу без масла",
  "clientId": null          // null = записывается сам
}
```

**Логика в `AppointmentService.create()`:**

1. **Найти клиента**
   - Если `clientId` null и текущий пользователь CLIENT → клиент = текущий пользователь
   - Если `clientId` передан и текущий пользователь ADMIN/OWNER → клиент = переданный ID

2. **Проверить сотрудника**
   - Сотрудник существует?
   - Сотрудник активен (`employee.isActive()`)?

3. **Проверить услугу**
   - Услуга существует?
   - Услуга активна (`service.isActive()`)?

4. **Вычислить время окончания**
   ```java
   endTime = startTime.plusMinutes(service.getDurationMinutes());
   ```

5. **КРИТИЧЕСКИ ВАЖНО: Проверка конфликта времени**
   ```java
   if (appointmentRepository.hasConflict(employeeId, startTime, endTime)) {
       throw new RuntimeException("Выбранное время уже занято");
   }
   ```
   
   SQL запрос:
   ```sql
   SELECT COUNT(*) > 0 FROM appointments
   WHERE employee_id = :employeeId
   AND status NOT IN ('CANCELLED')
   AND start_time < :endTime AND end_time > :startTime
   ```

6. **Создать запись со статусом PENDING**
   ```java
   Appointment appointment = Appointment.builder()
       .client(client)
       .employee(employee)
       .service(service)
       .startTime(startTime)
       .endTime(endTime)
       .comment(comment)
       .status(PENDING)  // автоматически
       .build();
   ```

7. **Сохранить в БД**
   ```java
   appointmentRepository.save(appointment);
   ```

8. **Отправить Telegram уведомление сотруднику**
   ```java
   telegramNotificationService.notifyEmployeeAboutNewAppointment(appointment);
   ```

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
  "status": "PENDING",
  "comment": "прошу без масла",
  "createdAt": "2026-07-18T14:30:00"
}
```

---

### Шаг 2: Подтверждение записи (ADMIN/OWNER)

```http
PATCH /api/appointments/123/status?status=CONFIRMED
Authorization: Bearer JWT_TOKEN
```

Просто меняет статус на `CONFIRMED`.

```java
// AppointmentService.updateStatus()
appointment.setStatus(CONFIRMED);
appointmentRepository.save(appointment);
```

---

### Шаг 3: Завершение записи (ADMIN/OWNER)

```http
PATCH /api/appointments/123/status?status=COMPLETED
Authorization: Bearer JWT_TOKEN
```

Меняет статус на `COMPLETED`.

```java
appointment.setStatus(COMPLETED);
appointmentRepository.save(appointment);
```

**❌ ПРОБЛЕМА:** Сеанс абонемента НЕ списывается автоматически!

---

### Шаг 4: Отмена записи

```http
PATCH /api/appointments/123/cancel
Authorization: Bearer JWT_TOKEN
```

**Логика в `AppointmentService.cancel()`:**

1. Найти запись
2. Проверить права:
   - Если клиент → может отменить только свою запись
   - Если ADMIN/OWNER → может отменить любую
3. Проверить что статус != COMPLETED (нельзя отменить завершённую)
4. Установить статус на CANCELLED

```java
if (appointment.getStatus() == COMPLETED) {
    throw new RuntimeException("Нельзя отменить завершённую запись");
}
appointment.setStatus(CANCELLED);
```

---

## 🎫 ЖИЗНЕННЫЙ ЦИКЛ АБОНЕМЕНТА

### Шаг 1: Создание абонемента

```http
POST /api/subscriptions
Content-Type: application/json
Authorization: Bearer JWT_TOKEN

{
  "clientId": 5,           // null = сам себя
  "serviceId": 2,          // какая услуга
  "totalSessions": 10,     // сколько сеансов куплено
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "notes": "куплен по акции"
}
```

**Логика в `SubscriptionService.create()`:**

1. Найти клиента (аналогично запису)
2. Найти услугу
3. Проверить что услуга активна
4. Проверить что `expiryDate >= startDate`
5. Создать абонемент с `remainingSessions = totalSessions` и `status = ACTIVE`

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
  "status": "ACTIVE",
  "notes": "куплен по акции",
  "createdAt": "2026-07-18T09:00:00"
}
```

---

### Шаг 2: Использование сеанса (Списание)

```http
PATCH /api/subscriptions/1/use
Authorization: Bearer JWT_TOKEN
```

**Логика в `SubscriptionService.useSession()`:**

1. **Найти абонемент**
2. **Проверить права**
   - Клиент может использовать только свой абонемент
   - ADMIN/OWNER могут использовать любой

3. **Проверить что status = ACTIVE**
   ```java
   if (sub.getStatus() != ACTIVE) {
       throw new RuntimeException("Абонемент не активен");
   }
   ```

4. **Проверить срок действия**
   ```java
   if (sub.getExpiryDate().isBefore(LocalDate.now())) {
       sub.setStatus(EXPIRED);  // обновить статус
       throw new RuntimeException("Абонемент истёк " + sub.getExpiryDate());
   }
   ```

5. **Проверить что есть сеансы**
   ```java
   if (sub.getRemainingSessions() <= 0) {
       sub.setStatus(EXHAUSTED);  // обновить статус
       throw new RuntimeException("Сеансы исчерпаны");
   }
   ```

6. **Списать сеанс**
   ```java
   sub.setRemainingSessions(sub.getRemainingSessions() - 1);
   ```

7. **Если это последний сеанс → обновить статус**
   ```java
   if (sub.getRemainingSessions() == 0) {
       sub.setStatus(EXHAUSTED);
   }
   ```

8. **Сохранить**
   ```java
   subscriptionRepository.save(sub);
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
  "remainingSessions": 9,      // ← уменьшилось на 1
  "startDate": "2026-08-01",
  "expiryDate": "2026-12-31",
  "status": "ACTIVE",
  "notes": "куплен по акции",
  "createdAt": "2026-07-18T09:00:00"
}
```

---

### Шаг 3: Отмена абонемента (ADMIN/OWNER)

```http
PATCH /api/subscriptions/1/cancel
Authorization: Bearer JWT_TOKEN
```

Просто меняет статус на `CANCELLED`.

---

### Шаг 4: Автоматическое истечение (BATCH JOB)

```java
// SubscriptionService.expireOutdated()
@Transactional
public void expireOutdated() {
    LocalDate today = LocalDate.now();
    List<Subscription> expired = subscriptionRepository.findAll()
        .stream()
        .filter(s -> s.getStatus() == ACTIVE && s.getExpiryDate().isBefore(today))
        .toList();
    
    expired.forEach(s -> s.setStatus(EXPIRED));
    subscriptionRepository.saveAll(expired);
}
```

**⚠️ ВАЖНО:** Этот метод существует но **НЕ ЗАПУСКАЕТСЯ АВТОМАТИЧЕСКИ**!

Нужно добавить в конфиг:
```java
@Scheduled(cron = "0 0 1 * * *")  // каждый день в 01:00
public void expireSubscriptions() {
    subscriptionService.expireOutdated();
}
```

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ (TODO)

### 1. Абонемент НЕ списывается при создании записи

**Текущее поведение:**
- Клиент создаёт запись → запись создана со статусом PENDING
- Абонемент остаётся с тем же количеством сеансов

**Что нужно сделать:**

**Вариант A (рекомендуется) — При создании записи:**

В `CreateAppointmentRequest` добавить:
```java
private Long subscriptionId;  // какой абонемент использовать
```

В `AppointmentService.create()` добавить после создания записи:
```java
if (req.getSubscriptionId() != null) {
    subscriptionService.useSession(req.getSubscriptionId(), currentUserEmail);
}
```

**Вариант B — При завершении записи:**

В `AppointmentService.updateStatus()` добавить:
```java
if (newStatus == COMPLETED) {
    // Найти активный абонемент клиента на эту услугу
    Optional<Subscription> activeSubscription = 
        subscriptionRepository.findActiveByClientAndService(
            appointment.getClient(), 
            appointment.getService()
        );
    
    activeSubscription.ifPresent(sub -> 
        subscriptionService.useSession(sub.getId(), currentUserEmail)
    );
}
```

---

### 2. Абонемент НЕ показывается при создании записи

Нужно добавить новый endpoint:
```java
GET /api/subscriptions/client/{clientId}/service/{serviceId}
```

Это позволит фронтенду показать:
- "У вас есть активный абонемент на эту услугу"
- Количество оставшихся сеансов
- Кнопку "Использовать абонемент"

---

### 3. Автообновление статусов НЕ запускается

Добавить scheduled task:
```java
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {
    private final SubscriptionService subscriptionService;
    
    @Scheduled(cron = "0 0 1 * * *")  // каждый день в 01:00
    public void expireSubscriptions() {
        subscriptionService.expireOutdated();
    }
}
```

---

## 📊 ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ

### Сценарий 1: Просто запись (без абонемента)

```bash
# 1. Создать запись
curl -X POST http://localhost:8081/api/appointments \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "serviceId": 2,
    "startTime": "2026-08-15T10:00:00"
  }'

# Ответ: Appointment с id=123, status=PENDING

# 2. Админ подтверждает
curl -X PATCH http://localhost:8081/api/appointments/123/status?status=CONFIRMED \
  -H "Authorization: Bearer TOKEN"

# 3. После сеанса админ завершает
curl -X PATCH http://localhost:8081/api/appointments/123/status?status=COMPLETED \
  -H "Authorization: Bearer TOKEN"
```

---

### Сценарий 2: Запись с абонементом (ПРАВИЛЬНЫЙ ПУТЬ)

```bash
# 1. Клиент покупает абонемент
curl -X POST http://localhost:8081/api/subscriptions \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": 2,
    "totalSessions": 10,
    "startDate": "2026-08-01",
    "expiryDate": "2026-12-31"
  }'

# Ответ: Subscription с id=1, remainingSessions=10, status=ACTIVE

# 2. Клиент создаёт запись (БЕЗ автоматического списания!)
curl -X POST http://localhost:8081/api/appointments \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "serviceId": 2,
    "startTime": "2026-08-15T10:00:00"
  }'

# Ответ: Appointment с id=123

# 3. ВРУЧНУЮ списать сеанс (ВРЕМЕННОЕ РЕШЕНИЕ)
curl -X PATCH http://localhost:8081/api/subscriptions/1/use \
  -H "Authorization: Bearer CUSTOMER_TOKEN"

# Ответ: Subscription с remainingSessions=9

# 4. Админ подтверждает запись
curl -X PATCH http://localhost:8081/api/appointments/123/status?status=CONFIRMED \
  -H "Authorization: Bearer ADMIN_TOKEN"

# 5. Админ завершает запись
curl -X PATCH http://localhost:8081/api/appointments/123/status?status=COMPLETED \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

---

## 🏗️ ФАЙЛЫ В ПРОЕКТЕ

```
Entities:
  src/main/java/com/salon/entity/Appointment.java
  src/main/java/com/salon/entity/AppointmentStatus.java
  src/main/java/com/salon/entity/Subscription.java
  src/main/java/com/salon/entity/SubscriptionStatus.java

Services:
  src/main/java/com/salon/service/AppointmentService.java
  src/main/java/com/salon/service/SubscriptionService.java

Controllers:
  src/main/java/com/salon/controller/AppointmentController.java
  src/main/java/com/salon/controller/SubscriptionController.java

Repositories:
  src/main/java/com/salon/repository/AppointmentRepository.java
  src/main/java/com/salon/repository/SubscriptionRepository.java

DTOs:
  src/main/java/com/salon/dto/request/CreateAppointmentRequest.java
  src/main/java/com/salon/dto/request/CreateSubscriptionRequest.java
  src/main/java/com/salon/dto/response/AppointmentResponse.java
  src/main/java/com/salon/dto/response/SubscriptionResponse.java
```

---

## ✅ ПРОВЕРКА ПОНИМАНИЯ

Ответьте на вопросы:

1. **Что происходит когда клиент создаёт запись?**
   - Создаётся Appointment со статусом PENDING, сотруднику отправляется Telegram

2. **Может ли клиент запи сать на занятое время?**
   - Нет, проверяется конфликт времени `hasConflict()`

3. **Когда сеанс абонемента списывается?**
   - **ТОЛЬКО** когда клиент вызывает `PATCH /api/subscriptions/{id}/use`

4. **Автоматически ли списывается сеанс при COMPLETED?**
   - **НЕТ** — это нужно доработать

5. **Что происходит если абонемент истёк?**
   - Во время использования выбросится исключение "Абонемент истёк"
   - Статус обновится на EXPIRED

---

## 🎯 ДЕЙСТВИЯ ДЛЯ КОМАНДЫ

- [ ] Реализовать автоматическое списание при создании Appointment (Вариант A рекомендуется)
- [ ] Добавить ScheduledTask для expireSubscriptions()
- [ ] Добавить endpoint для получения активного абонемента по услуге
- [ ] На фронте показывать доступный абонемент при создании записи
- [ ] Написать тесты для логики конфликтов времени
- [ ] Написать тесты для логики истечения абонементов

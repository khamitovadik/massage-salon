# 📋 Запись С абонементом vs БЕЗ абонемента

## 🔄 ДВА ВАРИАНТА СОЗДАНИЯ ЗАПИСИ

### ❌ Вариант 1: БЕЗ АБОНЕМЕНТА (просто платная запись)

**Запрос:**
```bash
POST /api/appointments
Authorization: Bearer CUSTOMER_TOKEN

{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00"
  // subscriptionId НЕ указан
}
```

**Логика в AppointmentService.create():**
```java
Subscription subscription = null;  // ← инициализируем как null

if (req.getSubscriptionId() != null) {  // ← проверяем есть ли subscriptionId
    // ... (этот блок НЕ выполняется)
}
// subscription остается null!

Appointment appointment = Appointment.builder()
    .subscription(subscription)  // subscription = null
    .status(PENDING)
    .build();
```

**Ответ API:**
```json
{
  "id": 123,
  "clientName": "Адилет",
  "employeeName": "Айгуль",
  "serviceName": "Массаж спины",
  "servicePrice": 5000.00,
  "status": "PENDING",
  "subscriptionId": null,         // ← NULL
  "subscriptionStatus": null,     // ← NULL
  "startTime": "2026-08-15T10:00:00",
  "endTime": "2026-08-15T11:00:00",
  "createdAt": "2026-07-18T14:30:00"
}
```

**При подтверждении:**
```bash
PATCH /api/appointments/123/status?status=CONFIRMED
```

```java
// В AppointmentService.updateStatus()
if (newStatus == AppointmentStatus.CONFIRMED && appointment.getSubscription() != null) {
    // ← это условие FALSE! (subscription == null)
    // Этот блок НЕ выполняется
}

// Просто меняется статус:
appointment.setStatus(CONFIRMED);
return ResponseEntity.ok(appointmentResponse);
```

**Результат:**
- ✅ Запись подтверждена
- ❌ Никаких сеансов не списано
- 💰 Клиент платит полную цену услуги

---

### ✅ Вариант 2: С АБОНЕМЕНТОМ (использует купленный пакет)

**Запрос:**
```bash
POST /api/appointments
Authorization: Bearer CUSTOMER_TOKEN

{
  "employeeId": 1,
  "serviceId": 2,
  "startTime": "2026-08-15T10:00:00",
  "subscriptionId": 1    // ← указываем какой абонемент использовать
}
```

**Логика в AppointmentService.create():**
```java
Subscription subscription = null;

if (req.getSubscriptionId() != null) {  // ← TRUE! (subscriptionId = 1)
    subscription = subscriptionRepository.findById(1);  // ← находим абонемент
    
    // ПРОВЕРКИ:
    if (!subscription.getClient().getId().equals(client.getId())) {
        throw new RuntimeException("Абонемент не принадлежит этому клиенту");
    }
    if (!subscription.getService().getId().equals(service.getId())) {
        throw new RuntimeException("Услуга не соответствует абонементу");
    }
    if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
        throw new RuntimeException("Абонемент не активен");
    }
    if (subscription.getExpiryDate().isBefore(LocalDate.now())) {
        throw new RuntimeException("Абонемент истёк");
    }
    if (subscription.getRemainingSessions() <= 0) {
        throw new RuntimeException("Нет доступных сеансов");
    }
}

Appointment appointment = Appointment.builder()
    .subscription(subscription)  // subscription = объект с id=1
    .status(PENDING)
    .build();
```

**Ответ API:**
```json
{
  "id": 124,
  "clientName": "Адилет",
  "employeeName": "Айгуль",
  "serviceName": "Массаж спины",
  "servicePrice": 5000.00,
  "status": "PENDING",
  "subscriptionId": 1,            // ← ID АБОНЕМЕНТА
  "subscriptionStatus": "ACTIVE", // ← СТАТУС АБОНЕМЕНТА
  "startTime": "2026-08-15T10:00:00",
  "endTime": "2026-08-15T11:00:00",
  "createdAt": "2026-07-18T14:30:00"
}
```

**При подтверждении:**
```bash
PATCH /api/appointments/124/status?status=CONFIRMED
```

```java
// В AppointmentService.updateStatus()
if (newStatus == AppointmentStatus.CONFIRMED && appointment.getSubscription() != null) {
    // ← это условие TRUE! (subscription существует)
    
    Subscription sub = appointment.getSubscription();  // subscription = объект
    
    if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getRemainingSessions() > 0) {
        int remainingBefore = sub.getRemainingSessions();  // было 10
        sub.setRemainingSessions(sub.getRemainingSessions() - 1);  // стало 9
        
        if (sub.getRemainingSessions() == 0) {
            sub.setStatus(SubscriptionStatus.EXHAUSTED);  // если это был последний
        }
        
        subscriptionRepository.save(sub);  // сохраняем изменения
        log.info("✅ Сеанс списан. Было: {}, Осталось: {}", 
            remainingBefore, sub.getRemainingSessions());
    }
}

return ResponseEntity.ok(appointmentResponse);
```

**Результат:**
- ✅ Запись подтверждена
- ✅ **АВТОМАТИЧЕСКИ списан 1 сеанс из абонемента** (10 → 9)
- ✅ Клиент не платит, используется абонемент

---

## 📊 СРАВНИТЕЛЬНАЯ ТАБЛИЦА

| Параметр | БЕЗ абонемента | С абонементом |
|----------|------------------|-----------------|
| **subscriptionId в запросе** | ❌ не указан | ✅ указан |
| **subscription в БД** | NULL | ID абонемента |
| **Проверки при создании** | Только время и сотрудник | + проверка абонемента |
| **Статус при создании** | PENDING | PENDING |
| **subscriptionId в ответе** | null | 1 (пример) |
| **При подтверждении** | Просто меняется статус | **Списывается сеанс** |
| **remainingSessions** | ❌ не меняется | ✅ уменьшается на 1 |
| **Оплата** | 💰 Полная цена | ✅ Бесплатно (из абонемента) |

---

## 🎯 КОГДА ЧТО ИСПОЛЬЗОВАТЬ

### Используйте БЕЗ АБОНЕМЕНТА когда:
- Клиент хочет разовую сеанс
- Клиент платит за каждый сеанс отдельно
- Не нужна предоплата
- `subscriptionId` не передаётся в запросе

### Используйте С АБОНЕМЕНТОМ когда:
- Клиент купил пакет сеансов
- Абонемент уже одобрен (status = ACTIVE)
- У клиента остались доступные сеансы
- Передаём `subscriptionId` в запросе

---

## 💻 ПРИМЕРЫ ТЕСТИРОВАНИЯ

### Сценарий А: Просто запись (без абонемента)

```bash
# 1. Создать запись БЕЗ абонемента
curl -X POST http://localhost:8081/api/appointments \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "serviceId": 2,
    "startTime": "2026-08-15T10:00:00"
  }'

# Ответ: 
# {
#   "id": 123,
#   "status": "PENDING",
#   "subscriptionId": null,
#   "subscriptionStatus": null
# }

# 2. Админ подтверждает
curl -X PATCH http://localhost:8081/api/appointments/123/status?status=CONFIRMED \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Ответ:
# {
#   "id": 123,
#   "status": "CONFIRMED",
#   "subscriptionId": null,
#   "subscriptionStatus": null
# }

# 3. Запись подтверждена, клиент должен заплатить 5000 руб за услугу
```

---

### Сценарий Б: Запись с абонементом

```bash
# 1. Абонемент уже одобрен (status: ACTIVE, remainingSessions: 10)

# 2. Создать запись С абонементом
curl -X POST http://localhost:8081/api/appointments \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "serviceId": 2,
    "startTime": "2026-08-15T10:00:00",
    "subscriptionId": 1
  }'

# Ответ:
# {
#   "id": 124,
#   "status": "PENDING",
#   "subscriptionId": 1,
#   "subscriptionStatus": "ACTIVE"
# }

# 3. Админ подтверждает
curl -X PATCH http://localhost:8081/api/appointments/124/status?status=CONFIRMED \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Ответ:
# {
#   "id": 124,
#   "status": "CONFIRMED",
#   "subscriptionId": 1,
#   "subscriptionStatus": "ACTIVE"
# }

# 4. Проверить что сеанс списан
curl -X GET http://localhost:8081/api/subscriptions/1 \
  -H "Authorization: Bearer CUSTOMER_TOKEN"

# Ответ:
# {
#   "id": 1,
#   "totalSessions": 10,
#   "remainingSessions": 9,    ← УМЕНЬШИЛОСЬ!
#   "status": "ACTIVE"
# }

# 5. Запись подтверждена, абонемент используется, клиент не платит
```

---

## 🔍 КЛЮЧЕВЫЕ МОМЕНТЫ В КОДЕ

### Проверка в create():
```java
if (req.getSubscriptionId() != null) {  // ← вот здесь решается
    // если null → эта часть пропускается, subscription остается null
    // если не null → проверяем и присваиваем subscription
}
```

### Проверка в updateStatus():
```java
if (newStatus == AppointmentStatus.CONFIRMED && appointment.getSubscription() != null) {
    // ← вот здесь происходит списание
    // если subscription == null → не списывается
    // если subscription != null → списывается сеанс
}
```

---

## ✅ SUMMARY

| | БЕЗ абонемента | С абонементом |
|-|---|---|
| Создание | Просто указываем время | Указываем subscriptionId |
| Проверка | Минимальная | Полная проверка абонемента |
| Подтверждение | Статус меняется | Статус меняется + сеанс списывается |
| Платёж | 💰 Полный | ✅ Бесплатно из абонемента |
| subscription в БД | NULL | ID абонемента |

**Оба варианта работают и поддерживаются! Выбирайте в зависимости от того, платит ли клиент напрямую или использует абонемент.** ✅

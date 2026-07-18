# 📅 ENDPOINTS ДЛЯ РАСПИСАНИЯ И ГРАФИКОВ

## 🎯 ДВА ТИПА ENDPOINTS

### ❌ ТИП 1: `/graph/` — ВСЕ записи (для анализа)

Возвращает **ВСЕ** записи, включая:
- 🟡 PENDING (ожидают подтверждения)
- 🟢 CONFIRMED (подтверждены)
- ✅ COMPLETED (завершены)
- 🔴 CANCELLED (отменены)

**Используйте для:** Аналитики, просмотра всех попыток записей, отладки

---

### ✅ ТИП 2: `/schedule/` — ТОЛЬКО подтвержденные (для расписания)

Возвращает **ТОЛЬКО подтвержденные**:
- 🟢 CONFIRMED (подтверждены)
- ✅ COMPLETED (завершены)

**Используйте для:** Отображения расписания, графика работы

---

## 📋 ПОЛНЫЙ СПИСОК ENDPOINTS

### 1️⃣ АДМИН/OWNER: Все записи администратора

#### ❌ ВСЕ записи (включая PENDING, CANCELLED)
```bash
GET /api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
Authorization: Bearer ADMIN_TOKEN
```

**Ответ:** Массив ВСЕ записей за июль (включая неподтвержденные)
```json
[
  { "id": 1, "status": "PENDING", "startTime": "2026-07-10T09:00:00", ... },
  { "id": 2, "status": "CONFIRMED", "startTime": "2026-07-10T10:30:00", ... },
  { "id": 3, "status": "CANCELLED", "startTime": "2026-07-15T14:00:00", ... },
  ...
]
```

#### ✅ ТОЛЬКО подтвержденные (для расписания)
```bash
GET /api/appointments/schedule/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
Authorization: Bearer ADMIN_TOKEN
```

**Ответ:** Массив ТОЛЬКО CONFIRMED и COMPLETED записей
```json
[
  { "id": 2, "status": "CONFIRMED", "startTime": "2026-07-10T10:30:00", ... },
  { "id": 4, "status": "COMPLETED", "startTime": "2026-07-12T15:00:00", ... },
  { "id": 5, "status": "CONFIRMED", "startTime": "2026-07-15T11:00:00", ... }
]
```

---

### 2️⃣ СОТРУДНИК: Свои записи

#### ❌ ВСЕ записи конкретного сотрудника
```bash
GET /api/appointments/graph/employee/1?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
Authorization: Bearer EMPLOYEE_TOKEN
```

**Ответ:** ВСЕ записи сотрудника с ID=1 (включая неподтвержденные)

#### ✅ ТОЛЬКО подтвержденные записи (расписание сотрудника)
```bash
GET /api/appointments/schedule/employee/1?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
Authorization: Bearer EMPLOYEE_TOKEN
```

**Ответ:** ТОЛЬКО CONFIRMED и COMPLETED записи сотрудника
```json
[
  {
    "id": 2,
    "status": "CONFIRMED",
    "employeeName": "Айгуль",
    "clientName": "Адилет",
    "serviceName": "Массаж спины",
    "startTime": "2026-07-10T10:30:00",
    "endTime": "2026-07-10T11:30:00"
  },
  {
    "id": 4,
    "status": "COMPLETED",
    "employeeName": "Айгуль",
    "clientName": "Мария",
    "serviceName": "Релаксирующий массаж",
    "startTime": "2026-07-12T15:00:00",
    "endTime": "2026-07-12T16:00:00"
  }
]
```

---

## 📊 СРАВНЕНИЕ ENDPOINTS

| Endpoint | Кто | Все записи | PENDING | CONFIRMED | COMPLETED | CANCELLED | Использование |
|----------|-----|-----------|---------|-----------|-----------|-----------|----------------|
| `/graph/all` | ADMIN/OWNER | ✅ | ✅ | ✅ | ✅ | ✅ | Аналитика |
| `/schedule/all` | ADMIN/OWNER | ❌ | ❌ | ✅ | ✅ | ❌ | 📅 Расписание админа |
| `/graph/employee/{id}` | ADMIN/OWNER/EMPLOYEE | ✅ | ✅ | ✅ | ✅ | ✅ | Анализ записей сотр. |
| `/schedule/employee/{id}` | ADMIN/OWNER/EMPLOYEE | ❌ | ❌ | ✅ | ✅ | ❌ | 📅 Расписание сотр. |

---

## 🔑 КЛЮЧЕВЫЕ РАЗЛИЧИЯ

### `/graph/` endpoints
```java
// Возвращает ВСЕ записи
return appointmentRepository.findAllInRange(from, to)
    .stream()
    .map(AppointmentResponse::from)
    .toList();
```

**Включает:** PENDING, CONFIRMED, COMPLETED, CANCELLED

---

### `/schedule/` endpoints (НОВЫЕ!)
```java
// Возвращает ТОЛЬКО подтвержденные
return appointmentRepository.findAllInRange(from, to).stream()
    .filter(a -> a.getStatus() == CONFIRMED || a.getStatus() == COMPLETED)
    .map(AppointmentResponse::from)
    .toList();
```

**Включает:** ТОЛЬКО CONFIRMED и COMPLETED

---

## 💻 ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ

### Пример 1: Админ хочет увидеть РАСПИСАНИЕ на июль

```bash
# Используем /schedule/all - только подтвержденные
curl -X GET "http://localhost:8081/api/appointments/schedule/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59" \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Получит только подтвержденные и завершенные записи
# PENDING и CANCELLED не будут показаны
```

---

### Пример 2: Сотрудник смотрит свой график на неделю

```bash
# Используем /schedule/employee/{id} - только его подтвержденные записи
curl -X GET "http://localhost:8081/api/appointments/schedule/employee/1?from=2026-07-08T00:00:00&to=2026-07-14T23:59:59" \
  -H "Authorization: Bearer EMPLOYEE_TOKEN"

# Получит расписание сотрудника на неделю
# Все его подтвержденные и завершенные записи в этот период
```

---

### Пример 3: Админ анализирует ВСЕ попытки записей (для отладки)

```bash
# Используем /graph/all - все записи без фильтра
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59" \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Получит ВСЕ записи:
# - PENDING (которые еще ждут подтверждения)
# - CONFIRMED (подтвержденные)
# - COMPLETED (завершенные)
# - CANCELLED (отмененные)
# Полезно для аналитики и отладки
```

---

### Пример 4: Фронтенд получает расписание для календаря

```javascript
// Использовать /schedule/ endpoints для отображения календаря!
const from = '2026-07-01T00:00:00';
const to = '2026-07-31T23:59:59';

// ДЛЯ АДМИНА - все подтвержденные записи
const response = await fetch(
  `/api/appointments/schedule/all?from=${from}&to=${to}`,
  {
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

// ДЛЯ СОТРУДНИКА - его подтвержденные записи
const response = await fetch(
  `/api/appointments/schedule/employee/1?from=${from}&to=${to}`,
  {
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

const appointments = await response.json();
// [
//   { "id": 2, "status": "CONFIRMED", "clientName": "Адилет", ... },
//   { "id": 4, "status": "COMPLETED", "clientName": "Мария", ... }
// ]
```

---

## 📅 СТАРЫЕ VS НОВЫЕ ENDPOINTS

### ❌ СТАРЫЕ (всё ещё работают, но показывают ВСЕ)
- `GET /api/appointments/graph/all` - все записи
- `GET /api/appointments/graph/employee/{id}` - все записи сотрудника

### ✅ НОВЫЕ (только подтвержденные, для расписания)
- `GET /api/appointments/schedule/all` - подтвержденные записи АДМИНА
- `GET /api/appointments/schedule/employee/{id}` - подтвержденные записи СОТРУДНИКА

---

## 🎯 КОГДА ЧТО ИСПОЛЬЗОВАТЬ

| Задача | Endpoint | Причина |
|--------|----------|---------|
| Показать расписание админу | `/schedule/all` | Только подтвержденные |
| Показать график сотрудника | `/schedule/employee/{id}` | Только его работу |
| Вывести календарь | `/schedule/*` | Только подтвержденные |
| Аналитика всех попыток | `/graph/all` | ВСЕ записи включая PENDING |
| Отладить почему запись не видна | `/graph/all` | Увидим PENDING и CANCELLED |

---

## ✅ ИТОГОВЫЙ РЕКОМЕНДАЦИЯ

**Для фронтенда при отображении расписания/календаря:**

```
ВСЕГДА используйте /schedule/ endpoints, а не /graph/!
```

**Потому что:**
- ✅ Показывают только подтвержденные записи
- ✅ Нет PENDING (ещё не подтвержденные)
- ✅ Нет CANCELLED (отмененные)
- ✅ Чистое расписание

```
Используйте /graph/ ТОЛЬКО для аналитики и отладки
```

---

## 🚀 ПРИМЕР ПРАВИЛЬНОГО ИСПОЛЬЗОВАНИЯ

### React компонент для календаря:

```javascript
import { useEffect, useState } from 'react';

function ScheduleCalendar({ employeeId, month, year }) {
  const [appointments, setAppointments] = useState([]);

  useEffect(() => {
    const from = `${year}-${String(month).padStart(2, '0')}-01T00:00:00`;
    const to = `${year}-${String(month).padStart(2, '0')}-31T23:59:59`;

    // ✅ ИСПОЛЬЗУЕМ /schedule/ - только подтвержденные
    const url = employeeId 
      ? `/api/appointments/schedule/employee/${employeeId}?from=${from}&to=${to}`
      : `/api/appointments/schedule/all?from=${from}&to=${to}`;

    fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => setAppointments(data));
  }, [employeeId, month, year]);

  return (
    <div>
      {appointments.map(apt => (
        <div key={apt.id}>
          <p>{apt.clientName} - {apt.startTime}</p>
        </div>
      ))}
    </div>
  );
}
```

---

## 📝 ФАЙЛЫ КОТОРЫЕ БЫЛИ ИЗМЕНЕНЫ

✅ `AppointmentController.java` - добавлены endpoints `/schedule/all` и `/schedule/employee/{id}`
✅ `AppointmentService.java` - добавлены методы `getConfirmedAppointmentsBetween()` и `getConfirmedEmployeeAppointmentsBetween()`

---

Вот теперь у вас есть чистые endpoints для расписания! 📅✅

# 📊 График/Расписание записей (Schedule Graph API)

## 🎯 Что добавлено

Вы теперь можете получать **записи как график/календарь** для отображения на фронте. Добавлены 2 новых endpoint'а с данными, удобными для визуализации:

---

## 🔌 API Endpoints

### 1️⃣ **Получить ВСЕ записи за период** (для админа)

```
GET /api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
```

**Требования:** `ADMIN` или `OWNER` роль

**Параметры:**
- `from` — дата/время начала (ISO 8601): `2026-07-01T00:00:00`
- `to` — дата/время окончания (ISO 8601): `2026-07-31T23:59:59`

**Ответ:**
```json
[
  {
    "id": 101,
    "clientId": 5,
    "clientName": "Петр Иванов",
    "clientPhone": "+7-999-123-45-67",
    "employeeId": 2,
    "employeeName": "Иван Сидоров",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 1,
    "serviceName": "Классический массаж",
    "servicePrice": 3500.00,
    "durationMinutes": 60,
    "startTime": "2026-07-10T09:00:00",
    "endTime": "2026-07-10T10:00:00",
    "status": "CONFIRMED",
    "comment": null,
    "createdAt": "2026-07-08T14:30:00"
  },
  {
    "id": 102,
    "clientId": 6,
    "clientName": "Мария Петрова",
    "clientPhone": "+7-999-234-56-78",
    "employeeId": 2,
    "employeeName": "Иван Сидоров",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 2,
    "serviceName": "Антицеллюлитный массаж",
    "servicePrice": 4500.00,
    "durationMinutes": 90,
    "startTime": "2026-07-10T10:30:00",
    "endTime": "2026-07-10T12:00:00",
    "status": "PENDING",
    "comment": "Есть проблема со спиной",
    "createdAt": "2026-07-09T10:15:00"
  }
]
```

---

### 2️⃣ **Получить записи конкретного сотрудника за период**

```
GET /api/appointments/graph/employee/{employeeId}?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
```

**Параметры:**
- `employeeId` — ID сотрудника (в пути URL)
- `from` — дата/время начала
- `to` — дата/время окончания

**Требования:** `ADMIN`, `OWNER` или сам `EMPLOYEE`

**Пример:**
```
GET /api/appointments/graph/employee/2?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
```

**Ответ:** Массив записей (как в endpoint'е выше, но только для этого сотрудника)

---

## 💻 Примеры использования

### cURL

**Все записи за июль:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8081/api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59"
```

**Записи сотрудника 2 за неделю:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8081/api/appointments/graph/employee/2?from=2026-07-08T00:00:00&to=2026-07-14T23:59:59"
```

### JavaScript/React

```javascript
// Получить все записи за период
const fetchGraphAppointments = async (from, to) => {
  const response = await fetch(
    `/api/appointments/graph/all?from=${from}&to=${to}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  const appointments = await response.json();
  return appointments;
};

// Пример использования
const from = '2026-07-01T00:00:00';
const to = '2026-07-31T23:59:59';
const appointments = await fetchGraphAppointments(from, to);

// Отобразить на календаре
appointments.forEach(apt => {
  console.log(`${apt.startTime} - ${apt.endTime}: ${apt.clientName} у ${apt.employeeName}`);
});
```

### React Component (пример календаря)

```jsx
import React, { useState, useEffect } from 'react';

function AppointmentCalendar({ from, to, employeeId }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAppointments = async () => {
      const url = employeeId
        ? `/api/appointments/graph/employee/${employeeId}?from=${from}&to=${to}`
        : `/api/appointments/graph/all?from=${from}&to=${to}`;

      const response = await fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      const data = await response.json();
      setAppointments(data);
      setLoading(false);
    };

    fetchAppointments();
  }, [from, to, employeeId]);

  if (loading) return <div>Загрузка...</div>;

  return (
    <div className="calendar">
      {appointments.map(apt => (
        <div key={apt.id} className="appointment-slot" 
             style={{
               gridColumn: getColumn(apt.startTime),
               gridRow: getRow(apt.startTime)
             }}>
          <div className={`status-${apt.status.toLowerCase()}`}>
            <strong>{apt.employeeName}</strong>
            <p>{apt.clientName}</p>
            <small>{apt.startTime} → {apt.endTime}</small>
          </div>
        </div>
      ))}
    </div>
  );
}

export default AppointmentCalendar;
```

---

## 📋 Фильтры и сортировка

**Данные уже сортируются по startTime (по возрастанию)** в response'е.

На фронте вы можете дополнительно фильтровать:

```javascript
// Только CONFIRMED записи
const confirmed = appointments.filter(a => a.status === 'CONFIRMED');

// Только PENDING записи
const pending = appointments.filter(a => a.status === 'PENDING');

// Только конкретного сотрудника
const employeeAppointments = appointments.filter(a => a.employeeId === 2);

// Только конкретного клиента
const clientAppointments = appointments.filter(a => a.clientId === 5);

// Только конкретную услугу
const massageAppointments = appointments.filter(a => a.serviceId === 1);
```

---

## 📊 Использование с библиотеками для календаря

### React Calendar (react-big-calendar)

```jsx
import { Calendar, momentLocalizer } from 'react-big-calendar';
import moment from 'moment';

const localizer = momentLocalizer(moment);

function ScheduleCalendar({ appointments }) {
  const events = appointments.map(apt => ({
    id: apt.id,
    title: `${apt.clientName} - ${apt.serviceName}`,
    start: new Date(apt.startTime),
    end: new Date(apt.endTime),
    resourceId: apt.employeeId,
    resource: {
      employeeName: apt.employeeName,
      clientPhone: apt.clientPhone,
      status: apt.status
    }
  }));

  return (
    <Calendar
      localizer={localizer}
      events={events}
      startAccessor="start"
      endAccessor="end"
      style={{ height: '100vh' }}
    />
  );
}
```

### FullCalendar.io

```jsx
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';

function CalendarView({ appointments }) {
  const events = appointments.map(apt => ({
    id: apt.id.toString(),
    title: `${apt.clientName} (${apt.employeeName})`,
    start: apt.startTime,
    end: apt.endTime,
    backgroundColor: apt.status === 'CONFIRMED' ? '#4CAF50' : '#FFC107',
    extendedProps: {
      clientPhone: apt.clientPhone,
      employeeSpecialization: apt.employeeSpecialization,
      comment: apt.comment,
      price: apt.servicePrice
    }
  }));

  return (
    <FullCalendar
      plugins={[dayGridPlugin, timeGridPlugin]}
      initialView="timeGridWeek"
      events={events}
      height="100vh"
      headerToolbar={{
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay'
      }}
    />
  );
}
```

---

## 🔐 Безопасность

- Только **ADMIN/OWNER** могут видеть ВСЕ записи (`/graph/all`)
- **EMPLOYEE** может видеть только свои записи (`/graph/employee/{его_id}`)
- **CLIENT** может видеть только свои записи через `/my`

---

## 📝 Что изменилось в коде

### ✅ Добавлено:

1. **AppointmentController** — 2 новых endpoint'а:
   - `GET /api/appointments/graph/all` 
   - `GET /api/appointments/graph/employee/{employeeId}`

2. **AppointmentService** — 2 новых метода:
   - `getAppointmentsBetween()` — ВСЕ записи за период
   - `getEmployeeAppointmentsBetween()` — записи сотрудника за период

3. **AppointmentRepository** — новый query:
   - `findAllInRange()` — получить ВСЕ записи в диапазоне дат

---

## 🚀 Как скомпилировать и запустить

```bash
# Скомпилировать
mvn clean compile

# Запустить приложение
mvn spring-boot:run

# Или собрать JAR и запустить
mvn clean package
java -jar target/massage-salon-0.0.1-SNAPSHOT.jar
```

Приложение запустится на `http://localhost:8081`

---

## 🧪 Тестирование

### 1. Получить JWT токен (авторизация)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@salon.com","password":"admin123"}'
```

Ответ:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Успешная авторизация"
}
```

### 2. Использовать токен для получения записей

```bash
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## ❓ FAQ

**Q: Почему используется `findAllInRange()` вместо `findUpcoming()`?**
A: `findUpcoming()` возвращает только записи со статусом PENDING/CONFIRMED. Для графика нужны ВСЕ записи (включая COMPLETED, CANCELLED) чтобы показать полную историю.

**Q: Можно ли фильтровать по статусу?**
A: Пока нет в API, но вы можете фильтровать на фронте:
```javascript
const confirmed = appointments.filter(a => a.status === 'CONFIRMED');
```

**Q: Как отобразить график на неделю/месяц?**
A: Просто измените параметры `from` и `to`:
```
// На неделю
from=2026-07-08T00:00:00&to=2026-07-14T23:59:59

// На месяц
from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
```

**Q: Это микросервис?**
A: Нет, это добавлено в основное приложение. Если позже нужна архитектура микросервисов, можно выделить отдельный `schedule-service`.

---

## 📞 Контакты и поддержка

Если есть вопросы по использованию API, обратитесь к документации Spring Boot или проверьте логи приложения:
```
tail -f logs/massage-salon.log
```

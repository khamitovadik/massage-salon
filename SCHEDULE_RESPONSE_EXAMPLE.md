# 📅 ПРИМЕР ОТВЕТА ДЛЯ РАСПИСАНИЯ

## Запрос: Получить расписание админа на июль

```bash
GET /api/appointments/schedule/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
Authorization: Bearer ADMIN_TOKEN
```

---

## Ответ: ВСЕ подтвержденные записи сотрудников

```json
[
  {
    "id": 2,
    "clientId": 5,
    "clientName": "Адилет Хамитов",
    "clientPhone": "+77072302002",
    "employeeId": 1,
    "employeeName": "Айгуль Сейткали",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 2,
    "serviceName": "Массаж спины",
    "servicePrice": 5000.00,
    "durationMinutes": 60,
    "startTime": "2026-07-10T10:30:00",
    "endTime": "2026-07-10T11:30:00",
    "status": "CONFIRMED",
    "comment": "Проблема со спиной",
    "createdAt": "2026-07-09T10:15:00",
    "subscriptionId": null,
    "subscriptionStatus": null
  },
  {
    "id": 4,
    "clientId": 3,
    "clientName": "Мария Петрова",
    "clientPhone": "+77002223344",
    "employeeId": 1,
    "employeeName": "Айгуль Сейткали",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 1,
    "serviceName": "Релаксирующий массаж",
    "servicePrice": 4500.00,
    "durationMinutes": 90,
    "startTime": "2026-07-12T15:00:00",
    "endTime": "2026-07-12T16:30:00",
    "status": "COMPLETED",
    "comment": null,
    "createdAt": "2026-07-11T14:20:00",
    "subscriptionId": 1,
    "subscriptionStatus": "ACTIVE"
  },
  {
    "id": 5,
    "clientId": 7,
    "clientName": "Иван Сидоров",
    "clientPhone": "+77003334455",
    "employeeId": 2,
    "employeeName": "Рауль Карибов",
    "employeeSpecialization": "Спортивный массаж",
    "serviceId": 3,
    "serviceName": "Спортивный массаж",
    "servicePrice": 6000.00,
    "durationMinutes": 60,
    "startTime": "2026-07-15T09:00:00",
    "endTime": "2026-07-15T10:00:00",
    "status": "CONFIRMED",
    "comment": "После тренировки",
    "createdAt": "2026-07-14T16:45:00",
    "subscriptionId": null,
    "subscriptionStatus": null
  }
]
```

---

## Запрос: Получить расписание конкретного сотрудника

```bash
GET /api/appointments/schedule/employee/1?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
Authorization: Bearer ADMIN_TOKEN
# или
Authorization: Bearer EMPLOYEE_TOKEN (если это сам сотрудник)
```

---

## Ответ: ТОЛЬКО записи сотрудника Айгуль (ID=1)

```json
[
  {
    "id": 2,
    "clientId": 5,
    "clientName": "Адилет Хамитов",
    "clientPhone": "+77072302002",
    "employeeId": 1,
    "employeeName": "Айгуль Сейткали",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 2,
    "serviceName": "Массаж спины",
    "servicePrice": 5000.00,
    "durationMinutes": 60,
    "startTime": "2026-07-10T10:30:00",
    "endTime": "2026-07-10T11:30:00",
    "status": "CONFIRMED",
    "comment": "Проблема со спиной",
    "createdAt": "2026-07-09T10:15:00",
    "subscriptionId": null,
    "subscriptionStatus": null
  },
  {
    "id": 4,
    "clientId": 3,
    "clientName": "Мария Петрова",
    "clientPhone": "+77002223344",
    "employeeId": 1,
    "employeeName": "Айгуль Сейткали",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 1,
    "serviceName": "Релаксирующий массаж",
    "servicePrice": 4500.00,
    "durationMinutes": 90,
    "startTime": "2026-07-12T15:00:00",
    "endTime": "2026-07-12T16:30:00",
    "status": "COMPLETED",
    "comment": null,
    "createdAt": "2026-07-11T14:20:00",
    "subscriptionId": 1,
    "subscriptionStatus": "ACTIVE"
  }
]
```

---

## 📊 СТРУКТУРА ДАННЫХ В ОТВЕТЕ

### 📍 Информация о записи:
```json
{
  "id": 2,                           // ID записи
  "status": "CONFIRMED",             // CONFIRMED или COMPLETED
  "startTime": "2026-07-10T10:30:00", // Когда начинается
  "endTime": "2026-07-10T11:30:00",   // Когда кончается
  "comment": "Проблема со спиной",   // Примечание клиента
  "createdAt": "2026-07-09T10:15:00" // Когда создана
}
```

### 👤 Информация о клиенте:
```json
{
  "clientId": 5,
  "clientName": "Адилет Хамитов",
  "clientPhone": "+77072302002"
}
```

### 💼 Информация о сотруднике:
```json
{
  "employeeId": 1,
  "employeeName": "Айгуль Сейткали",
  "employeeSpecialization": "Классический массаж"
}
```

### 🛠️ Информация об услуге:
```json
{
  "serviceId": 2,
  "serviceName": "Массаж спины",
  "servicePrice": 5000.00,
  "durationMinutes": 60
}
```

### 🎫 Информация об абонементе (если использует):
```json
{
  "subscriptionId": 1,
  "subscriptionStatus": "ACTIVE"
}
// или null если запись БЕЗ абонемента
```

---

## 📱 КАК ИСПОЛЬЗОВАТЬ НА ФРОНТЕ

### React пример для календаря:

```javascript
import { useEffect, useState } from 'react';

function ScheduleCalendar({ month, year }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const from = `${year}-${String(month).padStart(2, '0')}-01T00:00:00`;
    const to = `${year}-${String(month).padStart(2, '0')}-31T23:59:59`;

    fetch(
      `/api/appointments/schedule/all?from=${from}&to=${to}`,
      {
        headers: { 'Authorization': `Bearer ${token}` }
      }
    )
      .then(res => res.json())
      .then(data => {
        setAppointments(data);
        setLoading(false);
      });
  }, [month, year]);

  if (loading) return <div>Загрузка расписания...</div>;

  return (
    <div className="schedule">
      <h2>Расписание на {month}/{year}</h2>
      <table>
        <thead>
          <tr>
            <th>Время</th>
            <th>Клиент</th>
            <th>Телефон</th>
            <th>Сотрудник</th>
            <th>Услуга</th>
            <th>Цена</th>
            <th>Статус</th>
            <th>Абонемент</th>
          </tr>
        </thead>
        <tbody>
          {appointments.map(apt => (
            <tr key={apt.id}>
              <td>{apt.startTime} - {apt.endTime}</td>
              <td>{apt.clientName}</td>
              <td>{apt.clientPhone}</td>
              <td>{apt.employeeName}</td>
              <td>{apt.serviceName}</td>
              <td>{apt.servicePrice} ₽</td>
              <td>
                <span className={`status-${apt.status.toLowerCase()}`}>
                  {apt.status}
                </span>
              </td>
              <td>
                {apt.subscriptionId 
                  ? `ID: ${apt.subscriptionId} (${apt.subscriptionStatus})` 
                  : 'Нет'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ScheduleCalendar;
```

---

## 🎯 ЧТО ВЫВОДИТСЯ В РАСПИСАНИИ

| Поле | Что показывать |
|------|-----------------|
| `id` | ID записи (для редактирования) |
| `startTime` - `endTime` | ⏰ Время сеанса |
| `clientName` + `clientPhone` | 👤 Кто записался |
| `employeeName` + `employeeSpecialization` | 💼 Кто проводит |
| `serviceName` | 🛠️ Какая услуга |
| `servicePrice` | 💰 Цена |
| `durationMinutes` | ⏱️ Длительность |
| `status` | ✅ Статус (CONFIRMED/COMPLETED) |
| `subscriptionId` | 🎫 Использует ли абонемент |
| `comment` | 📝 Примечание клиента |

---

## ✅ ВСЕ ДАННЫЕ УЖЕ ЕСТЬ!

Endpoint `/api/appointments/schedule/*` возвращает **ВСЕ необходимые данные** для отображения расписания:

- ✅ Время записи
- ✅ Клиент с телефоном
- ✅ Сотрудник со специализацией
- ✅ Услуга с ценой
- ✅ Статус
- ✅ Информация об абонементе
- ✅ Комментарий

Просто используйте эти данные на фронте для отображения! 📅

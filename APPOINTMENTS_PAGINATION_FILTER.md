# 📄 ПАГИНАЦИЯ И ФИЛЬТР ПО ДАТАМ ДЛЯ ЗАПИСЕЙ

## ✅ ЧТО ДОБАВЛЕНО

### Новые endpoints:

| Endpoint | Описание |
|----------|---------|
| `GET /api/appointments/my/paginated` | Мои записи с пагинацией и фильтром |
| `GET /api/appointments/paginated` | Все записи (ADMIN/OWNER) с пагинацией и фильтром |

---

## 📊 ПАРАМЕТРЫ ПАГИНАЦИИ

```
page=0          // Номер страницы (с 0)
size=10         // Размер страницы (записей на странице)
dateFrom=2026-07-01  // Дата начала (необязательно)
dateTo=2026-07-31    // Дата конца (необязательно)
```

---

## 💻 ПРИМЕРЫ ЗАПРОСОВ

### 1️⃣ Клиент: Мои записи (первая страница, по 10 на странице)

```bash
GET /api/appointments/my/paginated?page=0&size=10
Authorization: Bearer CUSTOMER_TOKEN
```

**Ответ:**
```json
{
  "content": [
    {
      "id": 2,
      "clientName": "Адилет",
      "employeeName": "Айгуль",
      "serviceName": "Массаж спины",
      "startTime": "2026-07-10T10:30:00",
      "status": "CONFIRMED",
      ...
    },
    {
      "id": 4,
      "clientName": "Адилет",
      "employeeName": "Айгуль",
      "serviceName": "Релаксирующий массаж",
      "startTime": "2026-07-12T15:00:00",
      "status": "COMPLETED",
      ...
    }
  ],
  "totalElements": 25,      // Всего записей в БД
  "totalPages": 3,          // Всего страниц (25 / 10 = 3)
  "currentPage": 0,         // Текущая страница
  "pageSize": 10,           // Размер страницы
  "hasNext": true,          // Есть ли следующая страница
  "hasPrevious": false      // Есть ли предыдущая страница
}
```

---

### 2️⃣ Клиент: Мои записи в июле (фильтр по датам)

```bash
GET /api/appointments/my/paginated?page=0&size=10&dateFrom=2026-07-01&dateTo=2026-07-31
Authorization: Bearer CUSTOMER_TOKEN
```

**Ответ:** Только записи в июле (с пагинацией)
```json
{
  "content": [
    {
      "id": 2,
      "startTime": "2026-07-10T10:30:00",  ← в июле
      ...
    }
  ],
  "totalElements": 5,       // 5 записей в июле
  "totalPages": 1,          // 1 страница (5 записей)
  "currentPage": 0,
  "pageSize": 10,
  "hasNext": false,
  "hasPrevious": false
}
```

---

### 3️⃣ Админ: Все записи с пагинацией

```bash
GET /api/appointments/paginated?page=0&size=20
Authorization: Bearer ADMIN_TOKEN
```

---

### 4️⃣ Админ: Все записи за период

```bash
GET /api/appointments/paginated?page=0&size=20&dateFrom=2026-07-10&dateTo=2026-07-20
Authorization: Bearer ADMIN_TOKEN
```

---

### 5️⃣ Переключение между страницами

```bash
# Страница 0 (1-10 записей)
GET /api/appointments/my/paginated?page=0&size=10

# Страница 1 (11-20 записей)
GET /api/appointments/my/paginated?page=1&size=10

# Страница 2 (21-30 записей)
GET /api/appointments/my/paginated?page=2&size=10
```

---

## 📱 REACT ПРИМЕР

### Компонент с пагинацией и фильтром:

```javascript
import React, { useState, useEffect } from 'react';

function AppointmentsTable() {
  const [appointments, setAppointments] = useState([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [loading, setLoading] = useState(false);

  // Загрузить записи при изменении page или dates
  useEffect(() => {
    fetchAppointments();
  }, [page, dateFrom, dateTo]);

  const fetchAppointments = async () => {
    setLoading(true);
    
    // Построить URL с параметрами
    let url = `/api/appointments/my/paginated?page=${page}&size=${pageSize}`;
    if (dateFrom) url += `&dateFrom=${dateFrom}`;
    if (dateTo) url += `&dateTo=${dateTo}`;

    try {
      const response = await fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await response.json();
      
      setAppointments(data.content);
      setTotalPages(data.totalPages);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="appointments-container">
      <h2>Мои записи</h2>

      {/* ФИЛЬТР ПО ДАТАМ */}
      <div className="filters">
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => {
            setDateFrom(e.target.value);
            setPage(0); // Вернуться на первую страницу
          }}
          placeholder="Дата от"
        />
        <input
          type="date"
          value={dateTo}
          onChange={(e) => {
            setDateTo(e.target.value);
            setPage(0); // Вернуться на первую страницу
          }}
          placeholder="Дата до"
        />
      </div>

      {/* ТАБЛИЦА С ЗАПИСЯМИ */}
      {loading ? (
        <p>Загрузка...</p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <th>Время</th>
                <th>Сотрудник</th>
                <th>Услуга</th>
                <th>Статус</th>
                <th>Цена</th>
              </tr>
            </thead>
            <tbody>
              {appointments.map(apt => (
                <tr key={apt.id}>
                  <td>{apt.startTime} - {apt.endTime}</td>
                  <td>{apt.employeeName}</td>
                  <td>{apt.serviceName}</td>
                  <td>
                    <span className={`status-${apt.status.toLowerCase()}`}>
                      {apt.status}
                    </span>
                  </td>
                  <td>{apt.servicePrice} ₽</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* ПАГИНАЦИЯ */}
          <div className="pagination">
            <button
              onClick={() => setPage(page - 1)}
              disabled={page === 0}
            >
              ← Назад
            </button>

            <span>
              Страница {page + 1} из {totalPages}
            </span>

            <button
              onClick={() => setPage(page + 1)}
              disabled={page >= totalPages - 1}
            >
              Вперед →
            </button>

            {/* Выбор размера страницы */}
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setPage(0);
              }}
            >
              <option value="5">5 на странице</option>
              <option value="10">10 на странице</option>
              <option value="20">20 на странице</option>
              <option value="50">50 на странице</option>
            </select>
          </div>

          <p className="info">
            Всего записей: {appointments.length > 0 ? 
              `${page * pageSize + 1}-${Math.min((page + 1) * pageSize, appointments.length)}` 
              : '0'}
          </p>
        </>
      )}
    </div>
  );
}

export default AppointmentsTable;
```

---

## 🎯 CSS ДЛЯ ПАГИНАЦИИ

```css
.pagination {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
  border-radius: 4px;
}

.pagination button:hover:not(:disabled) {
  background: #f0f0f0;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination select {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filters input {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
```

---

## 📊 СТРУКТУРА ОТВЕТА

```json
{
  "content": [              // Массив записей на текущей странице
    { ... },
    { ... }
  ],
  "totalElements": 50,      // ВСЕГО записей в БД (или после фильтра)
  "totalPages": 5,          // ВСЕГО страниц (50 / 10 = 5)
  "currentPage": 0,         // Текущая страница (0-indexed)
  "pageSize": 10,           // Размер страницы
  "hasNext": true,          // Есть ли ещё страницы?
  "hasPrevious": false      // Есть ли предыдущие страницы?
}
```

---

## ✅ ЧТО ПОЛУЧИЛОСЬ

| Что | Было | Теперь |
|-----|------|--------|
| Все записи на одной странице | ❌ Грузит всё | ✅ По 10/20/50 на странице |
| Фильтр по датам | ❌ Нет | ✅ Выбрать дату от и до |
| Навигация между страницами | ❌ Нет | ✅ Кнопки назад/вперед |
| Информация о пагинации | ❌ Нет | ✅ Страница X из Y |
| Размер страницы | ❌ Фиксированный | ✅ Выбирать 5/10/20/50 |

---

## 🚀 ГОТОВО!

Теперь в админ-панели "Записи" можно:
- ✅ Просматривать записи постранично (по 10/20 на странице)
- ✅ Фильтровать по датам (от и до)
- ✅ Навигировать между страницами
- ✅ Менять размер страницы
- ✅ Видеть общую информацию (Страница X из Y)

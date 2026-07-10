# ⚡ Quick Start: Тестирование новых endpoints

## 🎯 Цель
Быстро протестировать новые endpoints для получения графика записей.

---

## ✅ Шаг 1: Запустить приложение

```bash
cd C:\dev\massage-salon
mvn clean compile
mvn spring-boot:run
```

Ждите пока выведется:
```
Started MassageSalonApplication in 5.123 seconds
```

Приложение готово на `http://localhost:8081`

---

## ✅ Шаг 2: Получить JWT токен (авторизация)

### cURL
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@salon.com","password":"admin123"}'
```

### Ответ
```json
{
  "message": "Успешная авторизация",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBzYWxvbi5jb20iLCJpYXQiOjE3MjA1OTk5MjcsImV4cCI6MTcyMDY4NjMyN30.abc123xyz..."
}
```

**Сохраните токен в переменную:**
```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBzYWxvbi5jb20iLCJpYXQiOjE3MjA1OTk5MjcsImV4cCI6MTcyMDY4NjMyN30.abc123xyz..."
```

---

## ✅ Шаг 3: Получить ВСЕ записи за июль (Новый endpoint #1)

```bash
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### Ответ (пример)
```json
[
  {
    "id": 1,
    "clientId": 1,
    "clientName": "Александр Петров",
    "clientPhone": "+7-900-123-45-67",
    "employeeId": 1,
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
    "id": 2,
    "clientId": 2,
    "clientName": "Мария Иванова",
    "clientPhone": "+7-900-234-56-78",
    "employeeId": 1,
    "employeeName": "Иван Сидоров",
    "employeeSpecialization": "Классический массаж",
    "serviceId": 2,
    "serviceName": "Антицеллюлитный массаж",
    "servicePrice": 4500.00,
    "durationMinutes": 90,
    "startTime": "2026-07-10T10:30:00",
    "endTime": "2026-07-10T12:00:00",
    "status": "PENDING",
    "comment": "Проблема со спиной",
    "createdAt": "2026-07-09T10:15:00"
  }
]
```

---

## ✅ Шаг 4: Получить записи конкретного сотрудника (Новый endpoint #2)

```bash
# Получить записи сотрудника с ID=1 за июль
curl -X GET "http://localhost:8081/api/appointments/graph/employee/1?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### Ответ
Такой же формат, но только записи сотрудника с ID=1

---

## 📊 Примеры использования разных дат

### Записи на одну неделю
```bash
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-08T00:00:00&to=2026-07-14T23:59:59" \
  -H "Authorization: Bearer $TOKEN"
```

### Записи на один день
```bash
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-10T00:00:00&to=2026-07-10T23:59:59" \
  -H "Authorization: Bearer $TOKEN"
```

### Записи за конкретные часы (10:00-12:00)
```bash
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-10T10:00:00&to=2026-07-10T12:00:00" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🧪 Тестирование в Postman

### 1. Импортировать collection (опционально)

Создайте новый request:
- **Method:** GET
- **URL:** http://localhost:8081/api/appointments/graph/all
- **Params:**
  - Key: `from`, Value: `2026-07-01T00:00:00`
  - Key: `to`, Value: `2026-07-31T23:59:59`
- **Headers:**
  - Key: `Authorization`, Value: `Bearer YOUR_TOKEN`
  - Key: `Content-Type`, Value: `application/json`

### 2. Нажать Send

---

## ✅ Проверка результатов

### Результат УСПЕШЕН если:
✅ HTTP Status: `200 OK`  
✅ Response body содержит массив записей (может быть пустой `[]`)  
✅ Каждая запись содержит поля: `id`, `clientName`, `employeeName`, `startTime`, `endTime`, `status`

### Результат ОШИБКА если:
❌ HTTP Status: `401 Unauthorized` → JWT токен неверный или истек  
❌ HTTP Status: `403 Forbidden` → У пользователя нет роли ADMIN/OWNER  
❌ HTTP Status: `500 Internal Server Error` → Ошибка на сервере (смотрите логи)

---

## 📋 Общие ошибки и решения

### Ошибка: 401 Unauthorized
```
{"message":"Недействительный токен"}
```
**Решение:** Токен истек или неверный. Получите новый токен (шаг 2).

### Ошибка: 403 Forbidden
```
{"message":"Доступ запрещен"}
```
**Решение:** У пользователя нет роли ADMIN/OWNER. Используйте админский аккаунт.

### Ошибка: 400 Bad Request
```
{"message":"Invalid time format"}
```
**Решение:** Дата в неверном формате. Используйте ISO 8601: `2026-07-01T00:00:00`

### Ошибка: No results
```
[]
```
**Решение:** Это не ошибка. Записей нет за этот период. Проверьте дату.

---

## 🔍 Просмотр логов

### Linux/Mac
```bash
tail -f logs/massage-salon.log
```

### Windows (PowerShell)
```powershell
Get-Content logs/massage-salon.log -Tail 50 -Wait
```

Ищите логи вроде:
```
[INFO] Получение всех записей за период с 2026-07-01T00:00:00 по 2026-07-31T23:59:59
[DEBUG] Query: SELECT a FROM Appointment a WHERE a.startTime >= :from AND a.startTime <= :to...
[INFO] Результат: 2 записи найдено
```

---

## 🎨 Красивый вывод JSON (Python)

Если хотите красивый вывод JSON:

```bash
curl -X GET "http://localhost:8081/api/appointments/graph/all?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59" \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
```

Или с цветом:
```bash
curl ... | python -m json.tool | less -R
```

---

## 🧠 Что дальше?

После успешного тестирования:

1. ✅ **На фронте:** Использовать этот endpoint для отображения календаря
2. ✅ **Фильтрация:** Фильтровать результаты по статусу, сотруднику и т.д.
3. ✅ **Оптимизация:** Кешировать часто запрашиваемые данные (Redis)
4. ✅ **Analytics:** Считать статистику (кол-во записей, выручку и т.д.)

---

## 💻 Node.js пример

Если разрабатываете на Node.js:

```javascript
const token = 'YOUR_JWT_TOKEN';
const from = '2026-07-01T00:00:00';
const to = '2026-07-31T23:59:59';

fetch(
  `http://localhost:8081/api/appointments/graph/all?from=${from}&to=${to}`,
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
)
  .then(res => res.json())
  .then(data => console.log(data))
  .catch(err => console.error(err));
```

---

## 🎯 Готово!

Вы успешно протестировали новые endpoints. Теперь можно:
- ✅ Использовать на фронте
- ✅ Отобразить на календаре
- ✅ Фильтровать и сортировать
- ✅ Строить аналитику

**Happy coding! 🚀**

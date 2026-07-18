# Система управления салоном массажа

## Краткое описание
Полноценное веб-приложение для управления салоном массажа.  
Бэкенд: Java 17 + Spring Boot 3.2.5  
Фронтенд: React 18 + TypeScript + Vite  
База данных: PostgreSQL (Neon.tech — бесплатно)  

---

## Ссылки

| Сервис | URL |
|--------|-----|
| Фронтенд (Vercel) | https://massage-salon-dfr7.vercel.app |
| Бэкенд (Render) | https://massage-salon.onrender.com |
| GitHub | https://github.com/khamitovadik/massage-salon |
| Neon (БД) | https://console.neon.tech |

**Внимание:** Render (бэкенд) засыпает после 15 мин без активности. Первый запрос может ждать ~50 сек.

---

## Тестовые аккаунты

| Email | Пароль | Роль |
|-------|--------|------|
| admin@salon.ru | admin123 | OWNER |
| aigul@salon.ru | password | EMPLOYEE |
| alikon@gmail.com | (из БД) | EMPLOYEE |

---

## Роли пользователей

| Роль | Доступ |
|------|--------|
| CLIENT | Записи (свои), Абонементы, Услуги, Отзывы |
| EMPLOYEE | + Расписание |
| ADMIN | + Управление записями всех клиентов, Сотрудники, Услуги |
| OWNER | Всё включая Аналитику, Пользователи, Рассылка, Настройки |

**Как сменить роль:** Neon SQL Editor → `UPDATE users SET role = 'OWNER' WHERE email = '...';`

---

## Структура проекта

```
C:\dev\massage-salon\
├── Dockerfile                   # Docker образ для Render
├── render.yaml                  # Конфиг Render
├── pom.xml                      # Maven зависимости
├── src/main/
│   ├── java/com/salon/
│   │   ├── MassageSalonApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java      # JWT + CORS + roles
│   │   │   └── DataInitializer.java     # Начальные данные
│   │   ├── controller/
│   │   │   ├── AuthController.java      # /api/auth/login, /register
│   │   │   ├── AppointmentController.java
│   │   │   ├── EmployeeController.java
│   │   │   ├── SalonServiceController.java
│   │   │   ├── SubscriptionController.java
│   │   │   ├── ScheduleController.java
│   │   │   ├── ReviewController.java
│   │   │   ├── AnalyticsController.java
│   │   │   ├── BroadcastController.java
│   │   │   ├── SalonSettingsController.java
│   │   │   ├── UserController.java      # /api/users (управление ролями)
│   │   │   └── HealthController.java    # /api/health
│   │   ├── entity/
│   │   │   ├── User.java               # implements UserDetails
│   │   │   ├── Role.java               # CLIENT, EMPLOYEE, ADMIN, OWNER
│   │   │   ├── Appointment.java
│   │   │   ├── AppointmentStatus.java  # PENDING, CONFIRMED, COMPLETED, CANCELLED
│   │   │   ├── Employee.java
│   │   │   ├── EmployeeSchedule.java
│   │   │   ├── SalonService.java
│   │   │   ├── SalonSettings.java
│   │   │   ├── Subscription.java
│   │   │   ├── SubscriptionStatus.java
│   │   │   ├── Review.java
│   │   │   └── TimeBlock.java
│   │   ├── service/                    # Бизнес-логика
│   │   ├── repository/                 # Spring Data JPA
│   │   ├── dto/request/ + dto/response/
│   │   ├── security/
│   │   │   ├── jwt/JwtUtil.java
│   │   │   ├── filter/JwtAuthFilter.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── notification/
│   │   │   ├── TelegramPollingService.java   # polling каждые 5 сек
│   │   │   ├── TelegramNotificationService.java
│   │   │   └── ReminderScheduler.java        # напоминания за 24ч
│   │   └── exception/GlobalExceptionHandler.java
│   └── resources/
│       └── application.yml
└── frontend/
    ├── vite.config.ts
    ├── tsconfig.json
    ├── vercel.json                      # SPA routing
    └── src/
        ├── App.tsx                      # роуты
        ├── main.tsx
        ├── api/
        │   ├── client.ts               # axios + JWT interceptor + 401 redirect
        │   ├── auth.ts
        │   ├── appointments.ts
        │   ├── employees.ts
        │   ├── services.ts
        │   └── subscriptions.ts
        ├── store/auth.ts               # localStorage: token + user
        ├── types/index.ts
        ├── utils/format.ts
        ├── components/
        │   └── Layout.tsx              # sidebar + мобильный hamburger
        └── pages/
            ├── LoginPage.tsx
            ├── RegisterPage.tsx
            ├── DashboardPage.tsx
            ├── AppointmentsPage.tsx
            ├── NewAppointmentPage.tsx
            ├── EmployeesPage.tsx
            ├── ServicesPage.tsx
            ├── SubscriptionsPage.tsx
            ├── SchedulePage.tsx
            ├── ReviewsPage.tsx
            ├── AnalyticsPage.tsx
            ├── BroadcastPage.tsx
            ├── SettingsPage.tsx
            └── UsersPage.tsx           # управление ролями (только OWNER)
```

---

## API эндпоинты

### Auth
```
POST /api/auth/login      { email, password } → { token, id, name, email, role }
POST /api/auth/register   { name, email, phone, password }
```

### Appointments
```
GET    /api/appointments          # все (ADMIN/OWNER)
GET    /api/appointments/my       # свои
POST   /api/appointments          # создать
PATCH  /api/appointments/{id}/status?status=CONFIRMED  # (ADMIN/OWNER)
PATCH  /api/appointments/{id}/cancel
```

### Employees
```
GET    /api/employees
POST   /api/employees             (ADMIN/OWNER)
PUT    /api/employees/{id}        (ADMIN/OWNER)
DELETE /api/employees/{id}        (ADMIN/OWNER)
```

### Schedule
```
GET /api/schedule/employee/{id}           # расписание сотрудника на неделю
GET /api/schedule/slots?employeeId=&serviceId=&date=   # свободные слоты
POST /api/schedule/employee/{id}          (ADMIN/OWNER)
```

### Analytics
```
GET /api/analytics/summary?from=YYYY-MM-DD&to=YYYY-MM-DD   (ADMIN/OWNER)
```
Возвращает: totalAppointments, completedAppointments, cancelledAppointments,
totalRevenue, averageCheck, activeSubscriptions, uniqueClients, 
employeeLoad[], revenueByService[], appointmentsByDay[]

### Users (управление ролями)
```
GET   /api/users              # список всех (OWNER)
PATCH /api/users/{id}/role    { role: "EMPLOYEE" }   (OWNER)
GET   /api/users/me
PATCH /api/users/me/telegram  { chatId: "123456" }
```

### Reviews
```
GET  /api/reviews             # все (любой авторизованный)
POST /api/reviews             { appointmentId, rating, comment }
DELETE /api/reviews/{id}      (ADMIN/OWNER)
```

### Broadcast
```
POST /api/broadcast   { message, allClients: true }   (ADMIN/OWNER)
```

### Settings
```
GET /api/settings
PUT /api/settings     { name, address, phone, email, description, workingHours }
```

---

## Telegram бот

- Бот: `@MassageSalonAldikBot`
- Токен: `8793892295:AAFFgcOKMCwTToNSlHhKFai-cnR1WEhlf9w`
- Owner chat ID: `978503826`
- Режим: polling (каждые 5 сек)
- Команды бота: `/start` — приветствие + инструкция по привязке аккаунта
- Уведомления: при создании записи, подтверждении, напоминание за 24ч

---

## Переменные окружения (Render)

| Переменная | Значение |
|-----------|---------|
| DATABASE_URL | jdbc:postgresql://ep-soft-firefly-ah69seh2-pooler.c-3.us-east-1.aws.neon.tech/neondb?sslmode=require |
| DATABASE_USERNAME | neondb_owner |
| DATABASE_PASSWORD | npg_YNjmyUEQd38w |
| PORT | 8080 |

---

## Локальный запуск

### Бэкенд
```powershell
# Нужен Docker (для локальной PostgreSQL) или подключение к Neon
cd C:\dev\massage-salon
# В IntelliJ IDEA — запустить MassageSalonApplication
# Или:
mvn spring-boot:run
# Порт: 8081
```

### Фронтенд
```powershell
cd C:\dev\massage-salon\frontend
npm install
npm run dev
# Порт: 3000, прокси /api → localhost:8081
```

---

## Что сделано ✅

- Авторизация JWT (login/register)
- 4 роли: CLIENT, EMPLOYEE, ADMIN, OWNER
- Управление записями (CRUD + статусы)
- Управление сотрудниками
- Управление услугами
- Расписание сотрудников + свободные слоты
- Абонементы
- Отзывы + рейтинг
- Аналитика (выручка, нагрузка сотрудников, статистика)
- Telegram бот (уведомления + напоминания)
- Рассылка сообщений клиентам через Telegram
- Настройки салона
- Управление пользователями (смена ролей — только OWNER)
- Мобильная адаптация (hamburger меню, breakpoint 640px)
- Деплой: Render (бэкенд) + Vercel (фронтенд) + Neon (БД)

---

## Что ещё нужно сделать ❌

### Высокий приоритет
- [ ] **Онлайн-запись для клиентов** — публичная страница без логина, где клиент выбирает услугу → сотрудника → время → оставляет имя/телефон
- [ ] **Календарь записей** — визуальный календарь (по дням/неделям) для OWNER/ADMIN вместо таблицы
- [ ] **Страница профиля** — клиент может поменять имя, телефон, привязать Telegram
- [ ] **Уведомления в браузере** — push-уведомления когда приходит новая запись

### Средний приоритет
- [ ] **Фильтрация записей** — по статусу, дате, сотруднику, клиенту
- [ ] **Поиск** — по имени клиента, номеру телефона
- [ ] **Отчёты в Excel** — выгрузка аналитики за период
- [ ] **Фото сотрудников** — загрузка аватара для каждого сотрудника
- [ ] **Галерея/портфолио** — фото работ салона
- [ ] **Теги для услуг** — категории (спина, тело, лицо и т.д.)
- [ ] **История изменений** — лог кто и когда менял статус записи

### Мелкие улучшения
- [ ] **Пагинация** в таблице записей (сейчас грузятся все)
- [ ] **Подтверждение удаления** — диалог "вы уверены?" перед удалением
- [ ] **Тёмная тема**
- [ ] **Язык интерфейса** — переключение RU/KZ/EN
- [ ] **Статистика на дашборде** — показывать сегодняшние записи сразу
- [ ] **Email уведомления** — дублировать Telegram через email

### Технический долг
- [ ] Добавить Maven wrapper (`mvnw`) — сейчас бэкенд деплоится через Dockerfile
- [ ] Написать тесты (unit + integration)
- [ ] Flyway миграции вместо `ddl-auto: update`
- [ ] Rate limiting на API
- [ ] Логирование действий пользователей

---

## Известные баги (исправлены в последней версии)

- ~~CORS блокировал PATCH запросы~~ — исправлено, добавлен PATCH в SecurityConfig
- ~~`hasAuthority('OWNER')` не работал~~ — исправлено на `hasRole('OWNER')`
- ~~Аналитика не передавала даты~~ — исправлено, теперь передаёт текущий месяц
- ~~Отзывы недоступны для EMPLOYEE~~ — исправлено, открыт доступ для всех
- ~~Дублирующийся SecurityFilterConfig~~ — удалён

---

## Деплой (обновление)

```powershell
cd C:\dev\massage-salon
git add -A
git commit -m "описание изменений"
git push
# Render пересоберёт бэкенд автоматически (~5-10 мин)
# Vercel пересоберёт фронтенд автоматически (~1-2 мин)
```

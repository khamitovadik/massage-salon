# 🏗️ Монолит vs Микросервис: Когда переходить?

## 📊 Что вы сейчас имеете (Монолит)

```
┌─────────────────────────────────────────┐
│     Massage Salon (Spring Boot)         │
├─────────────────────────────────────────┤
│  Controllers:                           │
│  - AuthController                       │
│  - AppointmentController  ✅ ГРАФИК    │
│  - EmployeeController                   │
│  - ScheduleController                   │
│  - SubscriptionController               │
│  - SalonServiceController               │
│                                          │
│  Services:                              │
│  - AuthService                          │
│  - AppointmentService  ✅ getGraph()   │
│  - ScheduleService                      │
│  - EmployeeService                      │
│  - ...                                  │
│                                          │
│  Database:                              │
│  - PostgreSQL (appointments, users...)  │
│                                          │
│  Port: 8081                             │
└─────────────────────────────────────────┘
```

**Преимущества монолита:**
✅ Простая разработка  
✅ Легко тестировать  
✅ Легко развертывать (один JAR файл)  
✅ Хорошая производительность (нет сетевых задержек)

**Недостатки монолита:**
❌ Все компоненты в одном процессе  
❌ Сложно масштабировать отдельные части  
❌ Изменение в одной части может сломать другие  

---

## 🚀 Что такое микросервис (Microservice Architecture)

```
┌──────────────────────┐         ┌──────────────────────┐
│  Main Service (8081) │         │ Schedule Service     │
│  ┌────────────────┐  │         │ (8082) - NEW!        │
│  │ Auth           │  │         │ ┌──────────────────┐ │
│  │ Appointments   │  │◄────────┤►│ Graph Logic       │ │
│  │ Employees      │  │ HTTP    │ │ Schedule Queries  │ │
│  │ Services       │  │         │ │ Calendar Formats  │ │
│  │ Subscriptions  │  │         │ └──────────────────┘ │
│  └────────────────┘  │         │                      │
│                      │         │ Database:            │
│ Database:            │         │ (читает из 8081)     │
│ - PostgreSQL         │         │ или своя DB          │
└──────────────────────┘         └──────────────────────┘
```

**Преимущества микросервисов:**
✅ Независимые деплойменты  
✅ Масштабируемость (можно увеличить instances графика отдельно)  
✅ Разные команды работают независимо  
✅ Разные технологии (Java, Node, Python, Go)  
✅ Отказоустойчивость (падение графика не сломает основное приложение)

**Недостатки микросервисов:**
❌ Сложнее разрабатывать  
❌ Нужна коммуникация между сервисами (HTTP/REST, gRPC, Message Queue)  
❌ Сложнее тестировать  
❌ Сложнее отлаживать  
❌ Сетевые задержки  

---

## 🤔 Когда переходить на микросервис?

### Используйте **Монолит (текущий подход)** если:
✅ Проект в начальной стадии  
✅ Команда маленькая (1-5 разработчиков)  
✅ Нет требований к независимой масштабируемости  
✅ Функциональность тесно связана  
✅ Нет критичных требований к отказоустойчивости  

### Используйте **Микросервисы** если:
✅ Проект очень большой  
✅ Разные части приложения нужно масштабировать независимо  
✅ Разные команды разрабатывают разные части  
✅ Нужна высокая доступность (99.9% uptime)  
✅ Разные части имеют разные требования к БД  
✅ Частые обновления независимых компонентов  

---

## 📈 Как ваш проект может расти

### Этап 1: Монолит (СЕЙЧАС) 📍
```
Один Spring Boot приложение на 8081 порту
- Записи (appointments) - получаем через /api/appointments/graph/*
- Всё в одной БД
```

### Этап 2: Монолит + кеширование
```
Добавляем Redis для кеширования графиков
- Часто запрашиваемые графики кешируются
- Улучшается производительность
```

### Этап 3: Монолит с фоновыми задачами
```
Добавляем RabbitMQ/Kafka для асинхронной обработки
- Генерация отчетов в фоне
- Отправка уведомлений Telegram
```

### Этап 4: Микросервисная архитектура (ДАЛЕКО В БУДУЩЕМ)
```
┌─────────────────────────┐
│  API Gateway (8080)     │ Входная точка
│  (маршрутизирует все)   │
└────────────┬────────────┘
             │
      ┌──────┼──────┬──────────┐
      │      │      │          │
      ▼      ▼      ▼          ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐
│ Auth │ │Booking│ │Staff│ │ Schedule │
│(8081)│ │(8082)│ │(8083)│ │ (8084)   │
└──────┘ └──────┘ └──────┘ └──────────┘
   │        │        │         │
   └────┬───┴────┬───┴─────────┘
        │        │
        ▼        ▼
   ┌────────────────────────┐
   │  PostgreSQL (основная) │
   │  Redis (кеш)           │
   │  Message Queue (очереди)
   └────────────────────────┘
```

---

## 💾 Вариант: Микросервис для графика

Если вы СЕЙЧАС хотите начать с микросервиса (что может быть рано, но технически возможно):

### Структура Schedule-Service

```
schedule-service/
├── pom.xml
│   ├── spring-boot-starter-web
│   ├── spring-boot-starter-data-jpa
│   ├── postgresql
│   └── retrofit2 (или RestTemplate для HTTP клиента)
├── src/main/java/com/salon/schedule/
│   ├── ScheduleServiceApplication.java
│   ├── controller/
│   │   └── ScheduleGraphController.java
│   ├── service/
│   │   └── ScheduleGraphService.java
│   ├── client/
│   │   └── MainServiceClient.java (HTTP клиент к основному приложению)
│   ├── dto/
│   │   ├── AppointmentDTO.java
│   │   └── GraphResponse.java
│   └── config/
│       └── RestClientConfig.java
├── src/main/resources/
│   └── application.yml
└── docker-compose.yml
```

### application.yml для schedule-service

```yaml
spring:
  application:
    name: schedule-service
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/massage_salon}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}

server:
  port: ${PORT:8084}

app:
  main-service-url: ${MAIN_SERVICE_URL:http://localhost:8081}
```

### HTTP клиент к основному приложению

```java
@Component
public class MainServiceClient {
    private final RestTemplate restTemplate;
    private final String mainServiceUrl;

    public MainServiceClient(RestTemplate restTemplate,
                           @Value("${app.main-service-url}") String mainServiceUrl) {
        this.restTemplate = restTemplate;
        this.mainServiceUrl = mainServiceUrl;
    }

    public List<AppointmentDTO> getAppointments(LocalDateTime from, LocalDateTime to) {
        String url = mainServiceUrl + "/api/appointments/graph/all" +
                "?from=" + from + "&to=" + to;
        
        try {
            ResponseEntity<List<AppointmentDTO>> response = 
                restTemplate.exchange(url, HttpMethod.GET, null, 
                    new ParameterizedTypeReference<List<AppointmentDTO>>(){});
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка получения данных с основного сервиса", e);
            throw new RuntimeException("Не удалось получить данные");
        }
    }
}
```

### ScheduleGraphController

```java
@RestController
@RequestMapping("/api/schedule-graph")
@RequiredArgsConstructor
public class ScheduleGraphController {
    private final MainServiceClient mainServiceClient;
    
    @GetMapping("/all")
    public ResponseEntity<List<?>> getAll(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(mainServiceClient.getAppointments(from, to));
    }
}
```

### docker-compose.yml для локальной разработки

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: massage_salon
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  main-service:
    build:
      context: ./massage-salon
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/massage_salon
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: postgres
    ports:
      - "8081:8081"
    depends_on:
      - postgres

  schedule-service:
    build:
      context: ./schedule-service
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/massage_salon
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: postgres
      MAIN_SERVICE_URL: http://main-service:8081
    ports:
      - "8084:8084"
    depends_on:
      - main-service
```

Запуск:
```bash
docker-compose up
```

---

## 🚦 Мой совет

### ✅ Делайте ЭТО (сейчас)
1. ✅ **Используйте текущий монолит** — добавляйте в основное приложение
2. ✅ **Добавляйте endpoint'ы для графика** — как вы уже сделали
3. ✅ **Кешируйте результаты** (Redis) — когда будут проблемы с производительностью
4. ✅ **Пишите unit тесты** — для новых методов

### ❌ Не делайте ЭТО (в начальной стадии)
1. ❌ **Не создавайте микросервисы рано** — вы потеряете 3 месяца на архитектуру
2. ❌ **Не усложняйте жизнь** — монолит намного проще в разработке
3. ❌ **Не используйте Kubernetes** — не нужно на этапе 1

### ✅ Переходите на микросервисы КОГДА:
- Приложение используют 100,000+ пользователей
- Нужна 99.9% доступность
- Разные компоненты требуют разных ресурсов
- У вас есть 5+ разработчиков
- Рост приложения остановился из-за монолита

---

## 📚 Дополнительно

**Мой рекомендуемый путь развития:**

```
Этап 1: Монолит (СЕЙЧАС)
   ↓ (когда начнутся проблемы с производительностью)
Этап 2: Монолит + Redis кеш
   ↓ (когда нужна масштабируемость)
Этап 3: Монолит + Kafka для асинхронной обработки
   ↓ (когда разные части требуют независимого масштабирования)
Этап 4: Микросервисы (API Gateway, отдельные сервисы)
   ↓ (когда команда выросла и нужна максимальная масштабируемость)
Этап 5: Kubernetes + Service Mesh (Istio)
```

**Вероятно, вы будете на Этапе 1 или 2 долгое время** — и это нормально! ✅

---

## 🎯 Итог

**Текущее решение (что я сделал):**
- ✅ Добавлены 2 endpoint'а для получения графика
- ✅ Используется существующая БД (нет лишних зависимостей)
- ✅ Простая реализация (30 строк кода)
- ✅ Готово к использованию на фронте
- ✅ Можно легко перейти на микросервис потом

**Это правильный подход для текущего этапа! 🚀**

# План реализации Schedule/Graph для Massage Salon

## 🎯 Цель
Вернуть данные записей в формате расписания/графика для отображения на фронте

## 📐 Вариант 1: Добавить ScheduleController в основное приложение (ПРОСТОЙ)

### Шаг 1: Создать DTO для расписания

**Файл:** `src/main/java/com/salon/dto/response/ScheduleSlotResponse.java`
```java
@Data
@Builder
public class ScheduleSlotResponse {
    private Long appointmentId;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String serviceName;
    private String clientName;
    private AppointmentStatus status;
    private int durationMinutes;
}
```

**Файл:** `src/main/java/com/salon/dto/response/DayScheduleResponse.java`
```java
@Data
@Builder
public class DayScheduleResponse {
    private LocalDate date;
    private Long employeeId;
    private String employeeName;
    private List<ScheduleSlotResponse> slots;  // занятые slots
    private List<LocalDateTime> availableSlots; // свободные слоты (опционально)
}
```

### Шаг 2: Создать сервис `ScheduleService`

**Файл:** `src/main/java/com/salon/service/ScheduleService.java`

```java
@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    
    // Получить расписание сотрудника на день
    public DayScheduleResponse getEmployeeScheduleForDay(Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        List<Appointment> appointments = appointmentRepository
            .findAllByEmployeeIdAndStartTimeBetween(employeeId, startOfDay, endOfDay);
        
        List<ScheduleSlotResponse> slots = appointments.stream()
            .map(ScheduleSlotResponse::from)
            .sorted(Comparator.comparing(ScheduleSlotResponse::getStartTime))
            .toList();
        
        return DayScheduleResponse.builder()
            .date(date)
            .employeeId(employeeId)
            .employeeName(employee.getUser().getFullName())
            .slots(slots)
            .build();
    }
    
    // Получить расписание на неделю
    public List<DayScheduleResponse> getEmployeeScheduleForWeek(Long employeeId, LocalDate weekStart) {
        List<DayScheduleResponse> weekSchedule = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekSchedule.add(getEmployeeScheduleForDay(employeeId, weekStart.plusDays(i)));
        }
        return weekSchedule;
    }
    
    // Получить расписание ВСЕХ сотрудников на день (для админа)
    public List<DayScheduleResponse> getAllEmployeesScheduleForDay(LocalDate date) {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
            .map(emp -> getEmployeeScheduleForDay(emp.getId(), date))
            .toList();
    }
}
```

### Шаг 3: Создать ScheduleController

**Файл:** `src/main/java/com/salon/controller/ScheduleController.java`

```java
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {
    
    private final ScheduleService scheduleService;
    
    /**
     * Расписание сотрудника на день
     * GET /api/schedule/employee/{employeeId}/day?date=2024-07-10
     */
    @GetMapping("/employee/{employeeId}/day")
    public ResponseEntity<DayScheduleResponse> getEmployeeDay(
            @PathVariable Long employeeId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(scheduleService.getEmployeeScheduleForDay(employeeId, date));
    }
    
    /**
     * Расписание сотрудника на неделю
     * GET /api/schedule/employee/{employeeId}/week?start=2024-07-08
     */
    @GetMapping("/employee/{employeeId}/week")
    public ResponseEntity<List<DayScheduleResponse>> getEmployeeWeek(
            @PathVariable Long employeeId,
            @RequestParam LocalDate start) {
        return ResponseEntity.ok(scheduleService.getEmployeeScheduleForWeek(employeeId, start));
    }
    
    /**
     * Расписание всех сотрудников на день (для админа)
     * GET /api/schedule/day?date=2024-07-10
     */
    @GetMapping("/day")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<DayScheduleResponse>> getAllEmployeesDay(
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(scheduleService.getAllEmployeesScheduleForDay(date));
    }
}
```

---

## 🚀 Вариант 2: Отдельный микросервис (ПРАВИЛЬНЕЕ, НО СЛОЖНЕЕ)

Если вам нужна настоящая микросервисная архитектура:

### Структура микросервиса:
```
schedule-service/
├── pom.xml (зависимости)
├── src/main/java/com/salon/schedule/
│   ├── ScheduleServiceApplication.java
│   ├── controller/
│   │   └── ScheduleController.java
│   ├── service/
│   │   └── ScheduleService.java
│   ├── client/
│   │   └── AppointmentClient.java (HTTP client к основному приложению)
│   └── dto/
│       └── (ScheduleSlotResponse, DayScheduleResponse и т.д.)
├── src/main/resources/
│   └── application.yml
└── docker-compose.yml (для локальной разработки)
```

### Коммуникация между сервисами:
- Основное приложение: `http://localhost:8081`
- Schedule сервис: `http://localhost:8082`
- Schedule сервис → HTTP REST клиент → основное приложение (GET /api/appointments)

### application.yml для микросервиса:
```yaml
spring:
  application:
    name: schedule-service
  
server:
  port: 8082

app:
  main-service-url: http://localhost:8081
```

---

## 🔄 Шаги реализации (Вариант 1 - рекомендуется):

1. ✅ **Обновить AppointmentRepository** - добавить методы для поиска по дате
   ```java
   List<Appointment> findAllByEmployeeIdAndStartTimeBetween(
       Long employeeId, LocalDateTime start, LocalDateTime end);
   ```

2. ✅ **Создать DTO** (ScheduleSlotResponse, DayScheduleResponse)

3. ✅ **Создать ScheduleService** с логикой группировки записей по дням

4. ✅ **Создать ScheduleController** с endpoint'ами

5. ✅ **Тестировать**:
   ```bash
   curl "http://localhost:8081/api/schedule/employee/1/day?date=2024-07-10"
   curl "http://localhost:8081/api/schedule/day?date=2024-07-10"
   ```

6. ✅ **На фронте** отобразить данные как календарь/график

---

## 📊 Пример ответа API:

```json
{
  "date": "2024-07-10",
  "employeeId": 1,
  "employeeName": "Иван Сидоров",
  "slots": [
    {
      "appointmentId": 101,
      "startTime": "2024-07-10T09:00:00",
      "endTime": "2024-07-10T10:00:00",
      "serviceName": "Классический массаж",
      "clientName": "Петр Иванов",
      "status": "CONFIRMED",
      "durationMinutes": 60
    },
    {
      "appointmentId": 102,
      "startTime": "2024-07-10T10:30:00",
      "endTime": "2024-07-10T11:30:00",
      "serviceName": "Антицеллюлитный массаж",
      "clientName": "Мария Петрова",
      "status": "PENDING",
      "durationMinutes": 60
    }
  ]
}
```

---

## 🎨 На фронте (пример для React):

```javascript
const fetchSchedule = async (employeeId, date) => {
  const response = await fetch(
    `/api/schedule/employee/${employeeId}/day?date=${date}`
  );
  const data = await response.json();
  
  // Отобразить как график/календарь
  renderCalendar(data.slots);
};
```

---

## 🤔 Когда переходить на Вариант 2 (микросервис)?

- Когда нужна независимая масштабируемость
- Когда логика обработки данных становится сложной
- Когда нужна отдельная история развертывания
- Когда много клиентов (фронтов) нужны данные расписания

## 📝 Рекомендация

**Начните с Варианта 1** — это займет 1-2 часа и даст вам работающее расписание. Потом, если нужно, перейдете на микросервис.

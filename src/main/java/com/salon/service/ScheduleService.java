package com.salon.service;

import com.salon.dto.response.HourSlotResponse;
import com.salon.entity.*;
import com.salon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    /** Часы работы по умолчанию, если у мастера не заполнено расписание на этот день */
    private static final LocalTime DEFAULT_START = LocalTime.of(11, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(23, 0);

    private final EmployeeScheduleRepository scheduleRepo;
    private final TimeBlockRepository timeBlockRepo;
    private final AppointmentRepository appointmentRepo;
    private final EmployeeRepository employeeRepo;
    private final SalonServiceRepository serviceRepo;
    private final EmployeeWorkDayRepository workDayRepo;

    /**
     * Получить расписание сотрудника (все 7 дней).
     */
    public List<EmployeeSchedule> getSchedule(Long employeeId) {
        return scheduleRepo.findAllByEmployeeId(employeeId);
    }

    /**
     * Сохранить расписание сотрудника.
     * Принимает список { dayOfWeek, working, workStart, workEnd }.
     */
    @Transactional
    public List<EmployeeSchedule> saveSchedule(Long employeeId, List<Map<String, Object>> days) {
        Employee employee = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        List<EmployeeSchedule> result = new ArrayList<>();
        for (Map<String, Object> d : days) {
            DayOfWeek dow = DayOfWeek.valueOf((String) d.get("dayOfWeek"));
            boolean working = Boolean.TRUE.equals(d.get("working"));

            EmployeeSchedule sch = scheduleRepo
                .findByEmployeeIdAndDayOfWeek(employeeId, dow)
                .orElseGet(() -> EmployeeSchedule.builder().employee(employee).dayOfWeek(dow).build());

            sch.setWorking(working);
            if (working && d.get("workStart") != null) {
                sch.setWorkStart(LocalTime.parse((String) d.get("workStart")));
                sch.setWorkEnd(LocalTime.parse((String) d.get("workEnd")));
            } else {
                sch.setWorkStart(null);
                sch.setWorkEnd(null);
            }
            result.add(scheduleRepo.save(sch));
        }
        return result;
    }

    /** Часы работы по датам (все сотрудники) за период — для календаря */
    public List<EmployeeWorkDay> getWorkDays(LocalDate from, LocalDate to) {
        return workDayRepo.findAllByWorkDateBetween(from, to);
    }

    /**
     * Задать часы работы сотрудника на дату. working=false — выходной.
     */
    @Transactional
    public EmployeeWorkDay saveWorkDay(Long employeeId, LocalDate date, boolean working, LocalTime start, LocalTime end) {
        Employee employee = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        if (working && (start == null || end == null || !start.isBefore(end))) {
            throw new RuntimeException("Начало работы должно быть раньше конца");
        }

        EmployeeWorkDay day = workDayRepo.findByEmployeeIdAndWorkDate(employeeId, date)
            .orElseGet(() -> EmployeeWorkDay.builder().employee(employee).workDate(date).build());
        day.setWorking(working);
        day.setWorkStart(working ? start : null);
        day.setWorkEnd(working ? end : null);
        return workDayRepo.save(day);
    }

    /** Убрать часы на дату — день снова идёт по недельному расписанию */
    @Transactional
    public void removeWorkDay(Long employeeId, LocalDate date) {
        workDayRepo.findByEmployeeIdAndWorkDate(employeeId, date).ifPresent(workDayRepo::delete);
    }

    /**
     * Добавить блокировку времени.
     */
    @Transactional
    public TimeBlock addBlock(Long employeeId, LocalDate date, LocalTime start, LocalTime end, String reason) {
        Employee employee = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        return timeBlockRepo.save(TimeBlock.builder()
            .employee(employee)
            .blockDate(date)
            .startTime(start)
            .endTime(end)
            .reason(reason)
            .build());
    }

    /**
     * Удалить блокировку.
     */
    @Transactional
    public void removeBlock(Long blockId) {
        timeBlockRepo.deleteById(blockId);
    }

    /**
     * Получить свободные слоты для записи.
     * employeeId — сотрудник, serviceId — услуга (нужна длительность), date — дата.
     */
    public List<String> getAvailableSlots(Long employeeId, Long serviceId, LocalDate date) {
        // 1. Рабочие часы на эту дату
        LocalTime[] hours = workingHours(employeeId, date);
        if (hours == null) {
            return List.of(); // выходной
        }

        // 2. Длительность услуги
        SalonService service = serviceRepo.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("Услуга не найдена"));
        int duration = service.getDurationMinutes();

        // 3. Существующие записи на этот день
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Appointment> dayAppointments = appointmentRepo.findUpcoming(dayStart, dayEnd).stream()
            .filter(a -> a.getEmployee().getId().equals(employeeId))
            .toList();

        // 4. Блокировки на этот день
        List<TimeBlock> blocks = timeBlockRepo.findAllByEmployeeIdAndBlockDate(employeeId, date);
        boolean fullDayBlocked = blocks.stream().anyMatch(b -> b.getStartTime() == null);
        if (fullDayBlocked) return List.of();

        // 5. Генерируем слоты с шагом 30 минут
        List<String> available = new ArrayList<>();
        LocalTime cursor = hours[0];
        LocalTime workEnd = hours[1];

        while (!cursor.plusMinutes(duration).isAfter(workEnd)) {
            LocalDateTime slotStart = date.atTime(cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(duration);

            boolean appointmentConflict = dayAppointments.stream().anyMatch(a ->
                a.getStartTime().isBefore(slotEnd) && a.getEndTime().isAfter(slotStart));

            boolean blockConflict = blocks.stream()
                .filter(b -> b.getStartTime() != null)
                .anyMatch(b -> {
                    LocalDateTime bStart = date.atTime(b.getStartTime());
                    LocalDateTime bEnd = date.atTime(b.getEndTime());
                    return bStart.isBefore(slotEnd) && bEnd.isAfter(slotStart);
                });

            boolean inPast = slotStart.isBefore(LocalDateTime.now());

            if (!appointmentConflict && !blockConflict && !inPast) {
                available.add(cursor.toString()); // "09:00", "09:30", ...
            }
            cursor = cursor.plusMinutes(30);
        }
        return available;
    }

    /**
     * Часовые окошки мастера на дату: свободные, занятые, недоступные и прошедшие.
     * staffView — показывать имя клиента и услугу в занятых окошках.
     */
    public List<HourSlotResponse> getHourlySlots(Long employeeId, LocalDate date, boolean staffView) {
        LocalTime[] hours = workingHours(employeeId, date);
        if (hours == null) return List.of();

        List<TimeBlock> blocks = timeBlockRepo.findAllByEmployeeIdAndBlockDate(employeeId, date);
        List<Appointment> dayAppointments = appointmentRepo
            .findAllInRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream()
            .filter(a -> a.getEmployee().getId().equals(employeeId))
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .toList();

        List<HourSlotResponse> slots = new ArrayList<>();
        LocalDateTime dayEnd = date.atTime(hours[1]);
        for (LocalDateTime start = date.atTime(hours[0]); !start.plusHours(1).isAfter(dayEnd); start = start.plusHours(1)) {
            LocalDateTime end = start.plusHours(1);
            HourSlotResponse.HourSlotResponseBuilder slot = HourSlotResponse.builder().time(start.toLocalTime().toString());

            LocalDateTime slotStart = start;
            Appointment busy = dayAppointments.stream()
                .filter(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(slotStart))
                .findFirst().orElse(null);

            if (busy != null) {
                slot.status("BUSY").appointmentId(staffView ? busy.getId() : null);
                if (staffView) {
                    slot.clientName(busy.getClient().getName()).serviceName(busy.getService().getName());
                }
            } else if (isBlocked(blocks, date, start, end)) {
                slot.status("BLOCKED");
            } else if (start.isBefore(LocalDateTime.now())) {
                slot.status("PAST");
            } else {
                slot.status("FREE");
            }
            slots.add(slot.build());
        }
        return slots;
    }

    /**
     * Проверить, что запись укладывается в рабочее время мастера, не попадает
     * на блокировку и не начинается в прошлом. Бросает RuntimeException с понятным текстом.
     */
    public void assertBookable(Long employeeId, LocalDateTime start, LocalDateTime end) {
        if (start.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Нельзя записаться на прошедшее время");
        }
        if (start.getMinute() != 0 || start.getSecond() != 0) {
            throw new RuntimeException("Запись возможна только на начало часа");
        }
        LocalDate date = start.toLocalDate();
        LocalTime[] hours = workingHours(employeeId, date);
        if (hours == null) {
            throw new RuntimeException("У мастера выходной в этот день");
        }
        if (start.toLocalTime().isBefore(hours[0]) || !end.toLocalDate().equals(date) || end.toLocalTime().isAfter(hours[1])) {
            throw new RuntimeException("Время вне рабочих часов мастера (" + hours[0] + "–" + hours[1] + ")");
        }
        if (isBlocked(timeBlockRepo.findAllByEmployeeIdAndBlockDate(employeeId, date), date, start, end)) {
            throw new RuntimeException("Мастер недоступен в это время");
        }
    }

    /**
     * Рабочие часы мастера на дату: [начало, конец] или null, если выходной.
     * Порядок: график на конкретную дату → недельное расписание → часы салона.
     */
    private LocalTime[] workingHours(Long employeeId, LocalDate date) {
        EmployeeWorkDay day = workDayRepo.findByEmployeeIdAndWorkDate(employeeId, date).orElse(null);
        if (day != null) {
            if (!day.isWorking() || day.getWorkStart() == null || day.getWorkEnd() == null) return null;
            return new LocalTime[]{day.getWorkStart(), day.getWorkEnd()};
        }
        EmployeeSchedule sch = scheduleRepo.findByEmployeeIdAndDayOfWeek(employeeId, date.getDayOfWeek()).orElse(null);
        if (sch == null) return new LocalTime[]{DEFAULT_START, DEFAULT_END};
        if (!sch.isWorking() || sch.getWorkStart() == null || sch.getWorkEnd() == null) return null;
        return new LocalTime[]{sch.getWorkStart(), sch.getWorkEnd()};
    }

    private boolean isBlocked(List<TimeBlock> blocks, LocalDate date, LocalDateTime start, LocalDateTime end) {
        return blocks.stream().anyMatch(b -> b.getStartTime() == null
            || (date.atTime(b.getStartTime()).isBefore(end) && date.atTime(b.getEndTime()).isAfter(start)));
    }
}

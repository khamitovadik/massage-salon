package com.salon.service;

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

    private final EmployeeScheduleRepository scheduleRepo;
    private final TimeBlockRepository timeBlockRepo;
    private final AppointmentRepository appointmentRepo;
    private final EmployeeRepository employeeRepo;
    private final SalonServiceRepository serviceRepo;

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
        // 1. Загружаем расписание на этот день недели
        DayOfWeek dow = date.getDayOfWeek();
        EmployeeSchedule sch = scheduleRepo.findByEmployeeIdAndDayOfWeek(employeeId, dow)
            .orElse(null);

        if (sch == null || !sch.isWorking() || sch.getWorkStart() == null) {
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
        LocalTime cursor = sch.getWorkStart();
        LocalTime workEnd = sch.getWorkEnd();

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
}

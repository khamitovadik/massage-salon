package com.salon.controller;

import com.salon.entity.EmployeeSchedule;
import com.salon.entity.TimeBlock;
import com.salon.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Расписание сотрудника (для отображения).
     * GET /api/schedule/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeSchedule>> getSchedule(@PathVariable Long employeeId) {
        return ResponseEntity.ok(scheduleService.getSchedule(employeeId));
    }

    /**
     * Сохранить расписание сотрудника — ADMIN/OWNER.
     * PUT /api/schedule/employee/{employeeId}
     * Body: [{"dayOfWeek":"MONDAY","working":true,"workStart":"09:00","workEnd":"18:00"}, ...]
     */
    @PutMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<EmployeeSchedule>> saveSchedule(
            @PathVariable Long employeeId,
            @RequestBody List<Map<String, Object>> days) {
        return ResponseEntity.ok(scheduleService.saveSchedule(employeeId, days));
    }

    /**
     * Свободные слоты для записи.
     * GET /api/schedule/slots?employeeId=1&serviceId=2&date=2026-07-01
     */
    @GetMapping("/slots")
    public ResponseEntity<List<String>> getSlots(
            @RequestParam Long employeeId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getAvailableSlots(employeeId, serviceId, date));
    }

    /**
     * Добавить блокировку времени — ADMIN/OWNER.
     * POST /api/schedule/employee/{employeeId}/block
     * Body: {"date":"2026-07-05","startTime":"13:00","endTime":"14:00","reason":"Обед"}
     */
    @PostMapping("/employee/{employeeId}/block")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<TimeBlock> addBlock(
            @PathVariable Long employeeId,
            @RequestBody Map<String, String> body) {
        LocalDate date = LocalDate.parse(body.get("date"));
        LocalTime start = body.get("startTime") != null ? LocalTime.parse(body.get("startTime")) : null;
        LocalTime end = body.get("endTime") != null ? LocalTime.parse(body.get("endTime")) : null;
        return ResponseEntity.ok(scheduleService.addBlock(employeeId, date, start, end, body.get("reason")));
    }

    /**
     * Удалить блокировку — ADMIN/OWNER.
     * DELETE /api/schedule/block/{blockId}
     */
    @DeleteMapping("/block/{blockId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> removeBlock(@PathVariable Long blockId) {
        scheduleService.removeBlock(blockId);
        return ResponseEntity.noContent().build();
    }
}

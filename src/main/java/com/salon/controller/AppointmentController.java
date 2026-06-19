package com.salon.controller;

import com.salon.dto.request.CreateAppointmentRequest;
import com.salon.dto.response.AppointmentResponse;
import com.salon.entity.AppointmentStatus;
import com.salon.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Создать запись — любой авторизованный пользователь
     * POST /api/appointments
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody CreateAppointmentRequest req,
            Authentication auth) {
        return ResponseEntity.ok(appointmentService.create(req, auth.getName()));
    }

    /**
     * Мои записи (для клиента — свои, для admin — можно использовать /all)
     * GET /api/appointments/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> my(Authentication auth) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(auth.getName()));
    }

    /**
     * Все записи — только ADMIN/OWNER
     * GET /api/appointments
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<AppointmentResponse>> getAll() {
        return ResponseEntity.ok(appointmentService.getAll());
    }

    /**
     * Записи конкретного сотрудника — ADMIN/OWNER/EMPLOYEE
     * GET /api/appointments/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','EMPLOYEE')")
    public ResponseEntity<List<AppointmentResponse>> byEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(appointmentService.getByEmployee(employeeId));
    }

    /**
     * Одна запись по id
     * GET /api/appointments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    /**
     * Изменить статус — ADMIN/OWNER
     * PATCH /api/appointments/{id}/status?status=CONFIRMED
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }

    /**
     * Отменить запись — клиент свою, ADMIN любую
     * PATCH /api/appointments/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(appointmentService.cancel(id, auth.getName()));
    }
}

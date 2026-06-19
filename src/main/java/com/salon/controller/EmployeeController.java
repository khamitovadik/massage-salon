package com.salon.controller;

import com.salon.dto.request.CreateEmployeeRequest;
import com.salon.dto.request.UpdateEmployeeRequest;
import com.salon.dto.response.EmployeeResponse;
import com.salon.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Создать сотрудника — только ADMIN или OWNER
     * POST /api/employees
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest req) {
        return ResponseEntity.ok(employeeService.create(req));
    }

    /**
     * Список активных сотрудников — любой авторизованный (для сайта)
     * GET /api/employees
     */
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getActive() {
        return ResponseEntity.ok(employeeService.getAllActive());
    }

    /**
     * Список всех сотрудников включая неактивных — ADMIN/OWNER
     * GET /api/employees/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        return ResponseEntity.ok(employeeService.getAll());
    }

    /**
     * Получить сотрудника по id
     * GET /api/employees/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    /**
     * Обновить сотрудника — ADMIN/OWNER
     * PUT /api/employees/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeRequest req) {
        return ResponseEntity.ok(employeeService.update(id, req));
    }

    /**
     * Деактивировать сотрудника — ADMIN/OWNER
     * DELETE /api/employees/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

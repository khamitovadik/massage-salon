package com.salon.controller;

import com.salon.dto.request.CreateEmployeeRequest;
import com.salon.dto.request.UpdateEmployeeRequest;
import com.salon.dto.response.EmployeeResponse;
import com.salon.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final com.salon.service.BranchService branchService;

    /**
     * Создать сотрудника — только ADMIN или OWNER
     * POST /api/employees
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest req, Authentication auth) {
        return ResponseEntity.ok(employeeService.create(req, auth.getName()));
    }

    /**
     * Список активных сотрудников — любой авторизованный (для сайта)
     * GET /api/employees
     */
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getActive(@RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(employeeService.getAllActive(branchId));
    }

    /**
     * Список всех сотрудников включая неактивных — ADMIN/OWNER
     * GET /api/employees/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<EmployeeResponse>> getAll(
            @RequestParam(required = false) Long branchId, Authentication auth) {
        return ResponseEntity.ok(employeeService.getAll(auth.getName(), branchId));
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
            @RequestBody UpdateEmployeeRequest req,
            Authentication auth) {
        employeeService.assertCanManage(id, auth.getName());
        return ResponseEntity.ok(employeeService.update(id, req));
    }

    /**
     * Деактивировать сотрудника — ADMIN/OWNER
     * DELETE /api/employees/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Authentication auth) {
        employeeService.assertCanManage(id, auth.getName());
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Филиалы мастера — владелец задаёт любые, руководитель меняет только свои.
     * PUT /api/employees/{id}/branches  Body: [branchId, ...]
     */
    @PutMapping("/{id}/branches")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<Long>> setBranches(@PathVariable Long id, @RequestBody List<Long> branchIds, Authentication auth) {
        return ResponseEntity.ok(branchService.setEmployeeBranches(id, branchIds, auth.getName()));
    }
}

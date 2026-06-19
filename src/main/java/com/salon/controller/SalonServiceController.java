package com.salon.controller;

import com.salon.dto.request.CreateServiceRequest;
import com.salon.dto.request.UpdateServiceRequest;
import com.salon.dto.response.ServiceResponse;
import com.salon.service.SalonServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class SalonServiceController {

    private final SalonServiceService salonServiceService;

    /**
     * Создать услугу — ADMIN/OWNER
     * POST /api/services
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest req) {
        return ResponseEntity.ok(salonServiceService.create(req));
    }

    /**
     * Список активных услуг — для всех (в т.ч. на сайте)
     * GET /api/services
     */
    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getActive() {
        return ResponseEntity.ok(salonServiceService.getAllActive());
    }

    /**
     * Все услуги включая архивные — ADMIN/OWNER
     * GET /api/services/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<ServiceResponse>> getAll() {
        return ResponseEntity.ok(salonServiceService.getAll());
    }

    /**
     * Одна услуга по id
     * GET /api/services/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salonServiceService.getById(id));
    }

    /**
     * Обновить услугу — ADMIN/OWNER
     * PUT /api/services/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateServiceRequest req) {
        return ResponseEntity.ok(salonServiceService.update(id, req));
    }

    /**
     * Деактивировать услугу — ADMIN/OWNER
     * DELETE /api/services/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        salonServiceService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

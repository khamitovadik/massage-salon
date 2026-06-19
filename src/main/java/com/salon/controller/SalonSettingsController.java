package com.salon.controller;

import com.salon.entity.SalonSettings;
import com.salon.service.SalonSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SalonSettingsController {

    private final SalonSettingsService settingsService;

    /** GET /api/settings — публичный, без авторизации */
    @GetMapping
    public ResponseEntity<SalonSettings> get() {
        return ResponseEntity.ok(settingsService.get());
    }

    /** PUT /api/settings — только OWNER */
    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SalonSettings> update(@RequestBody Map<String, String> fields) {
        return ResponseEntity.ok(settingsService.update(fields));
    }
}

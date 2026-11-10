package com.salon.controller;

import com.salon.dto.response.AnalyticsResponse;
import com.salon.dto.response.InsightsResponse;
import com.salon.service.InsightsService;
import com.salon.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final InsightsService insightsService;
    private final com.salon.service.BranchAccessService access;

    /**
     * GET /api/analytics/summary?from=2026-06-01&to=2026-06-30
     */
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(analyticsService.getSummary(from, to, access.scope(auth.getName(), branchId)));
    }

    /**
     * Расширенная аналитика с выводами: сравнение с прошлым периодом, клиенты, загрузка, отзывы.
     * GET /api/analytics/insights?from=2026-06-01&to=2026-06-30
     */
    @GetMapping("/insights")
    public ResponseEntity<InsightsResponse> getInsights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(insightsService.compute(from, to, access.scope(auth.getName(), branchId)));
    }
}

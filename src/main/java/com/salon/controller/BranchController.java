package com.salon.controller;

import com.salon.dto.response.BranchResponse;
import com.salon.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    /** Активные филиалы — открыто всем (сайт, запись). GET /api/branches */
    @GetMapping
    public ResponseEntity<List<BranchResponse>> getPublic() {
        return ResponseEntity.ok(branchService.getPublic());
    }

    /** Филиалы в панели: владельцу все (и закрытые), руководителю — свои. GET /api/branches/manage */
    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<BranchResponse>> getVisible(Authentication auth) {
        return ResponseEntity.ok(branchService.getVisible(auth.getName()));
    }

    /** Открыть новую точку — только владелец. Body: {name, address, phone, mapUrl, workingHours} */
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(branchService.create(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(branchService.update(id, body));
    }

    /** Закрыть точку (история сохраняется) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        branchService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** Назначить руководителей. Body: [userId, ...] */
    @PutMapping("/{id}/managers")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> setManagers(@PathVariable Long id, @RequestBody List<Long> userIds) {
        return ResponseEntity.ok(branchService.setManagers(id, userIds));
    }
}

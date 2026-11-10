package com.salon.controller;

import com.salon.dto.request.BroadcastRequest;
import com.salon.service.BroadcastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class BroadcastController {

    private final BroadcastService broadcastService;
    private final com.salon.service.BranchAccessService access;

    /**
     * Рассылка сообщения клиентам через Telegram.
     * POST /api/broadcast
     * Body: { "message": "Акция! 20% скидка...", "allClients": true }
     */
    @PostMapping
    public ResponseEntity<Map<String, Integer>> broadcast(
            @Valid @RequestBody BroadcastRequest req,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long branchId,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(broadcastService.broadcast(req.getMessage(), req.isAllClients(),
            access.scope(auth.getName(), branchId)));
    }
}

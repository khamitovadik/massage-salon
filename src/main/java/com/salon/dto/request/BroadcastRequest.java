package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastRequest {
    @NotBlank(message = "Сообщение не может быть пустым")
    private String message;

    /** Если true — отправить всем клиентам с chatId. Если false — только тем, у кого активные абонементы */
    private boolean allClients = true;
}

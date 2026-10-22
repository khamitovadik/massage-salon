package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Часовое окошко для записи.
 * status: FREE — свободно, BUSY — занято записью, BLOCKED — мастер недоступен, PAST — время прошло.
 * clientName/serviceName заполняются только для персонала.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourSlotResponse {

    private String time;
    private String status;
    private Long appointmentId;
    private String clientName;
    private String serviceName;
}

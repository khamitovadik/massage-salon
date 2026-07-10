package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayScheduleResponse {

    private LocalDate date;
    private Long employeeId;
    private String employeeName;
    private List<ScheduleSlotResponse> slots;
}

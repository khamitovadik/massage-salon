package com.salon.dto.request;

import lombok.Data;

@Data
public class UpdateEmployeeRequest {
    private String name;
    private String phone;
    private String specialization;
    private String description;
    private Boolean active;
}

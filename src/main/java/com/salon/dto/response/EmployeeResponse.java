package com.salon.dto.response;

import com.salon.entity.Employee;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String specialization;
    private String description;
    private boolean active;
    /** Филиалы мастера */
    private java.util.List<Long> branchIds;
    private java.util.List<String> branchNames;

    public static EmployeeResponse from(Employee e) {
        return EmployeeResponse.builder()
            .id(e.getId())
            .userId(e.getUser().getId())
            .name(e.getUser().getName())
            .email(e.getUser().getEmail())
            .phone(e.getUser().getPhone())
            .specialization(e.getSpecialization())
            .description(e.getDescription())
            .active(e.isActive())
            .branchIds(e.getBranches().stream().map(com.salon.entity.Branch::getId).sorted().toList())
            .branchNames(e.getBranches().stream()
                .sorted(java.util.Comparator.comparing(com.salon.entity.Branch::getId))
                .map(com.salon.entity.Branch::getName).toList())
            .build();
    }
}

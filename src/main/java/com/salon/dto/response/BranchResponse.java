package com.salon.dto.response;

import com.salon.entity.Branch;
import lombok.Builder;
import lombok.Data;

import java.util.Comparator;
import java.util.List;

@Data
@Builder
public class BranchResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String mapUrl;
    private String workingHours;
    private boolean active;
    /** Руководители — только для панели управления, на публичном сайте пусто */
    private List<Manager> managers;

    public record Manager(Long id, String name, String email) {}

    /** Для публичного сайта — без руководителей */
    public static BranchResponse publicFrom(Branch b) {
        return base(b).managers(List.of()).build();
    }

    public static BranchResponse from(Branch b) {
        return base(b).managers(b.getManagers().stream()
            .sorted(Comparator.comparing(u -> u.getName() == null ? "" : u.getName()))
            .map(u -> new Manager(u.getId(), u.getName(), u.getEmail()))
            .toList()).build();
    }

    private static BranchResponseBuilder base(Branch b) {
        return BranchResponse.builder()
            .id(b.getId())
            .name(b.getName())
            .address(b.getAddress())
            .phone(b.getPhone())
            .mapUrl(b.getMapUrl())
            .workingHours(b.getWorkingHours())
            .active(b.isActive());
    }
}

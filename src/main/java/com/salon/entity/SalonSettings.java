package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Единственная строка — настройки салона (id=1 всегда).
 */
@Entity
@Table(name = "salon_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonSettings {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;
    private String phone;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "working_hours")
    private String workingHours;

    @Column(name = "logo_url")
    private String logoUrl;
}

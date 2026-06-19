package com.salon.repository;

import com.salon.entity.SalonSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonSettingsRepository extends JpaRepository<SalonSettings, Long> {
}

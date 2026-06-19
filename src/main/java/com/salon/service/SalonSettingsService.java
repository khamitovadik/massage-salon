package com.salon.service;

import com.salon.entity.SalonSettings;
import com.salon.repository.SalonSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalonSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final SalonSettingsRepository repo;

    public SalonSettings get() {
        return repo.findById(SETTINGS_ID).orElseGet(() ->
            SalonSettings.builder()
                .id(SETTINGS_ID)
                .name("Массажный салон")
                .workingHours("Пн–Вс 09:00–21:00")
                .build()
        );
    }

    @Transactional
    public SalonSettings update(Map<String, String> fields) {
        SalonSettings s = get();
        if (fields.containsKey("name"))         s.setName(fields.get("name"));
        if (fields.containsKey("address"))      s.setAddress(fields.get("address"));
        if (fields.containsKey("phone"))        s.setPhone(fields.get("phone"));
        if (fields.containsKey("email"))        s.setEmail(fields.get("email"));
        if (fields.containsKey("description"))  s.setDescription(fields.get("description"));
        if (fields.containsKey("instagramUrl")) s.setInstagramUrl(fields.get("instagramUrl"));
        if (fields.containsKey("workingHours")) s.setWorkingHours(fields.get("workingHours"));
        if (fields.containsKey("logoUrl"))      s.setLogoUrl(fields.get("logoUrl"));
        return repo.save(s);
    }
}

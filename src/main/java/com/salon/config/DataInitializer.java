package com.salon.config;

import com.salon.entity.Role;
import com.salon.entity.SalonSettings;
import com.salon.entity.User;
import com.salon.repository.UserRepository;
import com.salon.service.SalonSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final SalonSettingsService settingsService;

    @Value("${app.owner.email}")
    private String ownerEmail;

    @Value("${app.owner.password:}")
    private String ownerPassword;

    @Override
    public void run(ApplicationArguments args) {
        // Hibernate создал CHECK по старому списку статусов, а ddl-auto=update его не обновляет —
        // без этого новый статус OFFERED не сохранится
        jdbcTemplate.execute("ALTER TABLE IF EXISTS subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check");

        // Номер салона для звонков и кнопок WhatsApp на лендинге; задаём только если в настройках пусто
        SalonSettings settings = settingsService.get();
        if (settings.getPhone() == null || settings.getPhone().isBlank()) {
            settingsService.update(Map.of("phone", "+7 747 364 13 96"));
            log.info("Телефон салона установлен: +7 747 364 13 96");
        }

        userRepository.findByEmail(ownerEmail).ifPresentOrElse(
            owner -> {
                owner.setActive(true);
                owner.setRole(Role.OWNER);
                // Пароль меняем только если он явно задан в OWNER_PASSWORD — иначе не трогаем
                if (!ownerPassword.isBlank()) {
                    owner.setPassword(passwordEncoder.encode(ownerPassword));
                    log.info("Пароль руководителя {} обновлён из OWNER_PASSWORD", ownerEmail);
                }
                userRepository.save(owner);
            },
            () -> {
                boolean generated = ownerPassword.isBlank();
                String password = generated ? randomPassword() : ownerPassword;
                userRepository.save(User.builder()
                    .name("Администратор")
                    .email(ownerEmail)
                    .phone("+70000000000")
                    .password(passwordEncoder.encode(password))
                    .role(Role.OWNER)
                    .build());
                if (generated) {
                    log.warn("Создан аккаунт руководителя {}. Пароль (показан один раз, смените его): {}", ownerEmail, password);
                } else {
                    log.info("Создан аккаунт руководителя {}", ownerEmail);
                }
            }
        );
    }

    private static String randomPassword() {
        String chars = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}

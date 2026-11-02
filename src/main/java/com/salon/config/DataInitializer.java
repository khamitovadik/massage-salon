package com.salon.config;

import com.salon.entity.Role;
import com.salon.entity.SalonSettings;
import com.salon.entity.User;
import com.salon.repository.UserRepository;
import com.salon.service.SalonSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final SalonSettingsService settingsService;

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

        userRepository.findByEmail("admin@salon.ru").ifPresentOrElse(
            admin -> {
                // Всегда обновляем пароль при старте — на случай если хеш устарел
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setActive(true);
                admin.setRole(Role.OWNER);
                userRepository.save(admin);
                log.info("Пароль руководителя обновлён: admin@salon.ru / admin123");
            },
            () -> {
                User admin = User.builder()
                    .name("Администратор")
                    .email("admin@salon.ru")
                    .phone("+70000000000")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.OWNER)
                    .build();
                userRepository.save(admin);
                log.info("Создан аккаунт руководителя: admin@salon.ru / admin123");
            }
        );
    }
}

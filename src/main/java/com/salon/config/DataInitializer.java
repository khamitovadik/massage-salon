package com.salon.config;

import com.salon.entity.Role;
import com.salon.entity.User;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
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

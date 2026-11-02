package com.salon.service;

import com.salon.entity.User;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    /**
     * Полное удаление пользователя. На него ссылаются записи, абонементы, отзывы и карточка
     * сотрудника — удаляем их по порядку внешних ключей, иначе БД не даст удалить строку users.
     */
    @Transactional
    public void deleteWithData(Long id, String currentEmail) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new RuntimeException("Нельзя удалить свой аккаунт");
        }

        String employeeIds = "SELECT id FROM employees WHERE user_id = ?";
        // Записи клиента, записи к нему как к мастеру и записи по его абонементам
        String appointmentWhere = "client_id = ? OR employee_id IN (" + employeeIds + ")"
            + " OR subscription_id IN (SELECT id FROM subscriptions WHERE client_id = ?)";

        jdbc.update("DELETE FROM reviews WHERE client_id = ? OR employee_id IN (" + employeeIds + ")"
            + " OR appointment_id IN (SELECT id FROM appointments WHERE " + appointmentWhere + ")",
            id, id, id, id, id);
        jdbc.update("DELETE FROM appointments WHERE " + appointmentWhere, id, id, id);
        jdbc.update("DELETE FROM subscriptions WHERE client_id = ?", id);
        jdbc.update("DELETE FROM employee_schedules WHERE employee_id IN (" + employeeIds + ")", id);
        jdbc.update("DELETE FROM time_blocks WHERE employee_id IN (" + employeeIds + ")", id);
        jdbc.update("DELETE FROM employees WHERE user_id = ?", id);
        jdbc.update("DELETE FROM users WHERE id = ?", id);

        log.info("Удалён пользователь {} ({})", user.getEmail(), id);
    }
}

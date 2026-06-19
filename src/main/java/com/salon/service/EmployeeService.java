package com.salon.service;

import com.salon.dto.request.CreateEmployeeRequest;
import com.salon.dto.request.UpdateEmployeeRequest;
import com.salon.dto.response.EmployeeResponse;
import com.salon.entity.Employee;
import com.salon.entity.Role;
import com.salon.entity.User;
import com.salon.repository.EmployeeRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Создать сотрудника: создаём User с ролью EMPLOYEE + Employee профиль */
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email уже занят");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new RuntimeException("Телефон уже занят");
        }

        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail())
            .phone(req.getPhone())
            .password(passwordEncoder.encode(req.getPassword()))
            .role(Role.EMPLOYEE)
            .build();
        userRepository.save(user);

        Employee employee = Employee.builder()
            .user(user)
            .specialization(req.getSpecialization())
            .description(req.getDescription())
            .build();
        employeeRepository.save(employee);

        return EmployeeResponse.from(employee);
    }

    /** Список всех активных сотрудников */
    public List<EmployeeResponse> getAllActive() {
        return employeeRepository.findAllByActiveTrue()
            .stream().map(EmployeeResponse::from).toList();
    }

    /** Все сотрудники (для ADMIN/OWNER — включая неактивных) */
    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll()
            .stream().map(EmployeeResponse::from).toList();
    }

    /** Один сотрудник по id */
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(findOrThrow(id));
    }

    /** Обновить данные сотрудника */
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest req) {
        Employee employee = findOrThrow(id);
        User user = employee.getUser();

        if (req.getName() != null)           user.setName(req.getName());
        if (req.getPhone() != null)          user.setPhone(req.getPhone());
        if (req.getSpecialization() != null) employee.setSpecialization(req.getSpecialization());
        if (req.getDescription() != null)    employee.setDescription(req.getDescription());
        if (req.getActive() != null)         employee.setActive(req.getActive());

        userRepository.save(user);
        employeeRepository.save(employee);
        return EmployeeResponse.from(employee);
    }

    /** Деактивировать сотрудника (soft delete) */
    @Transactional
    public void deactivate(Long id) {
        Employee employee = findOrThrow(id);
        employee.setActive(false);
        employee.getUser().setActive(false);
        employeeRepository.save(employee);
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден: " + id));
    }
}

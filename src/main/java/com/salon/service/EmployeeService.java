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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchAccessService access;
    private final BranchService branchService;
    private final com.salon.repository.BranchRepository branchRepository;

    /**
     * Создать сотрудника: создаём User с ролью EMPLOYEE + Employee профиль.
     * Если аккаунт с таким email уже есть (например, человек зарегистрировался сам) —
     * добавляем карточку мастера к нему; пароль при этом не меняем.
     */
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req, String currentEmail) {
        EmployeeResponse created = createProfile(req);
        assignBranches(created.getId(), req.getBranchIds(), currentEmail);
        return EmployeeResponse.from(findOrThrow(created.getId()));
    }

    /**
     * Филиалы нового мастера: указанные в запросе; иначе у руководителя — его филиалы;
     * иначе, если филиал всего один, — он.
     */
    private void assignBranches(Long employeeId, List<Long> requested, String currentEmail) {
        List<Long> ids = requested;
        if (ids == null || ids.isEmpty()) {
            Set<Long> scope = access.scope(currentEmail, null);
            if (scope != null) {
                ids = List.copyOf(scope);
            } else {
                var active = branchRepository.findAllByActiveTrueOrderByIdAsc();
                ids = active.size() == 1 ? List.of(active.get(0).getId()) : List.of();
            }
        }
        if (!ids.isEmpty()) branchService.setEmployeeBranches(employeeId, ids, currentEmail);
    }

    private EmployeeResponse createProfile(CreateEmployeeRequest req) {
        User existing = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (existing != null) {
            return attachToExistingUser(existing, req);
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

    private EmployeeResponse attachToExistingUser(User user, CreateEmployeeRequest req) {
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (employee != null && employee.isActive()) {
            throw new RuntimeException("Этот пользователь уже сотрудник");
        }
        userRepository.findByPhone(req.getPhone())
            .filter(other -> !other.getId().equals(user.getId()))
            .ifPresent(other -> { throw new RuntimeException("Телефон уже занят другим пользователем"); });

        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setActive(true);
        // Руководителя/админа не понижаем — они тоже могут принимать клиентов
        if (user.getRole() == Role.CLIENT) user.setRole(Role.EMPLOYEE);
        userRepository.save(user);

        // Деактивированного мастера возвращаем, а не создаём вторую карточку (user_id уникален)
        if (employee == null) employee = Employee.builder().user(user).build();
        employee.setSpecialization(req.getSpecialization());
        employee.setDescription(req.getDescription());
        employee.setActive(true);
        employeeRepository.save(employee);

        return EmployeeResponse.from(employee);
    }

    /** Список всех активных сотрудников */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllActive(Long branchId) {
        return employeeRepository.findAllByActiveTrue().stream()
            .filter(e -> branchId == null || e.getBranches().stream().anyMatch(b -> b.getId().equals(branchId)))
            .map(EmployeeResponse::from).toList();
    }

    /** Все сотрудники (для ADMIN/OWNER — включая неактивных) */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll(String email, Long branchId) {
        Set<Long> scope = access.scope(email, branchId);
        boolean admin = access.user(email).getRole() == Role.ADMIN;
        return employeeRepository.findAll().stream()
            .filter(e -> scope == null
                || e.getBranches().stream().anyMatch(b -> scope.contains(b.getId()))
                // мастера без филиала руководитель видит, чтобы добавить к себе
                || (admin && branchId == null && e.getBranches().isEmpty()))
            .map(EmployeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public void assertCanManage(Long id, String email) {
        access.assertCanManage(email, findOrThrow(id));
    }

    /** Один сотрудник по id */
    @Transactional(readOnly = true)
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

package com.salon.repository;

import com.salon.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findAllByActiveTrue();
    Optional<Employee> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}

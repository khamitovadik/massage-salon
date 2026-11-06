package com.salon.repository;

import com.salon.entity.EmployeeWorkDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeWorkDayRepository extends JpaRepository<EmployeeWorkDay, Long> {

    Optional<EmployeeWorkDay> findByEmployee_IdAndWorkDate(Long employeeId, LocalDate workDate);

    List<EmployeeWorkDay> findAllByWorkDateBetween(LocalDate from, LocalDate to);
}

package com.salon.repository;

import com.salon.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {

    List<TimeBlock> findAllByEmployeeIdAndBlockDate(Long employeeId, LocalDate blockDate);

    List<TimeBlock> findAllByEmployeeIdAndBlockDateBetween(Long employeeId, LocalDate from, LocalDate to);
}

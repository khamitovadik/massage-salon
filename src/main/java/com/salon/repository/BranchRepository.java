package com.salon.repository;

import com.salon.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findAllByOrderByIdAsc();

    List<Branch> findAllByActiveTrueOrderByIdAsc();

    /** Филиалы, которыми руководит пользователь */
    List<Branch> findAllByManagers_IdOrderByIdAsc(Long userId);
}

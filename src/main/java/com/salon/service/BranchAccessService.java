package com.salon.service;

import com.salon.entity.*;
import com.salon.repository.BranchRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Кто какие филиалы видит.
 * OWNER — все; ADMIN — только те, которыми руководит; остальным фильтр по филиалам не нужен.
 *
 * «Область» (scope) — множество id филиалов, по которым фильтруются данные; null означает «все».
 */
@Service
@RequiredArgsConstructor
public class BranchAccessService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public User user(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    /** id филиалов, которыми руководит пользователь */
    @Transactional(readOnly = true)
    public Set<Long> managedIds(Long userId) {
        return branchRepository.findAllByManagers_IdOrderByIdAsc(userId).stream()
            .map(Branch::getId).collect(Collectors.toSet());
    }

    /**
     * Область данных для запроса. branchId — выбранный в интерфейсе филиал (null — «все мои»).
     * Руководитель, запросивший чужой филиал, получает 403.
     */
    @Transactional(readOnly = true)
    public Set<Long> scope(String email, Long branchId) {
        User u = user(email);
        if (u.getRole() == Role.ADMIN) {
            Set<Long> mine = managedIds(u.getId());
            if (branchId == null) return mine;
            if (!mine.contains(branchId)) throw new AccessDeniedException("Чужой филиал");
            return Set.of(branchId);
        }
        return branchId == null ? null : Set.of(branchId);
    }

    public static boolean inScope(Set<Long> scope, Branch branch) {
        return scope == null || (branch != null && scope.contains(branch.getId()));
    }

    /** Может ли пользователь управлять филиалом */
    @Transactional(readOnly = true)
    public void assertManages(String email, Long branchId) {
        User u = user(email);
        if (u.getRole() == Role.OWNER) return;
        if (u.getRole() == Role.ADMIN && branchId != null && managedIds(u.getId()).contains(branchId)) return;
        throw new AccessDeniedException("Нет доступа к филиалу");
    }

    /** Руководитель работает с записью, только если она в его филиале */
    public void assertCanManage(String email, Appointment a) {
        assertManages(email, a.getBranch() != null ? a.getBranch().getId() : null);
    }

    /**
     * Руководитель может менять мастера, если тот работает хотя бы в одном его филиале
     * (или ещё ни к одному филиалу не привязан — чтобы его можно было добавить к себе).
     */
    @Transactional(readOnly = true)
    public void assertCanManage(String email, Employee e) {
        User u = user(email);
        if (u.getRole() == Role.OWNER) return;
        if (u.getRole() == Role.ADMIN) {
            if (e.getBranches().isEmpty()) return;
            Set<Long> mine = managedIds(u.getId());
            if (e.getBranches().stream().anyMatch(b -> mine.contains(b.getId()))) return;
        }
        throw new AccessDeniedException("Мастер работает в другом филиале");
    }

    /** Филиалы, которые пользователь видит в панели управления */
    @Transactional(readOnly = true)
    public List<Branch> visibleBranches(String email) {
        User u = user(email);
        if (u.getRole() == Role.ADMIN) return branchRepository.findAllByManagers_IdOrderByIdAsc(u.getId());
        return branchRepository.findAllByOrderByIdAsc();
    }
}

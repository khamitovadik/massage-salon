package com.salon.service;

import com.salon.dto.response.BranchResponse;
import com.salon.entity.Branch;
import com.salon.entity.Employee;
import com.salon.entity.Role;
import com.salon.entity.User;
import com.salon.repository.BranchRepository;
import com.salon.repository.EmployeeRepository;
import com.salon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchAccessService access;

    /** Активные филиалы для сайта и записи */
    @Transactional(readOnly = true)
    public List<BranchResponse> getPublic() {
        return branchRepository.findAllByActiveTrueOrderByIdAsc().stream().map(BranchResponse::publicFrom).toList();
    }

    /** Филиалы в панели: владельцу — все, руководителю — свои */
    @Transactional(readOnly = true)
    public List<BranchResponse> getVisible(String email) {
        return access.visibleBranches(email).stream().map(BranchResponse::from).toList();
    }

    @Transactional
    public BranchResponse create(Map<String, Object> body) {
        Branch b = new Branch();
        b.setActive(true);
        b.setManagers(new HashSet<>());
        b.setCreatedAt(java.time.LocalDateTime.now());
        apply(b, body);
        return BranchResponse.from(branchRepository.save(b));
    }

    @Transactional
    public BranchResponse update(Long id, Map<String, Object> body) {
        Branch b = find(id);
        boolean wasActive = b.isActive();
        apply(b, body);
        if (wasActive && !b.isActive()) assertNotLastActive();
        return BranchResponse.from(branchRepository.save(b));
    }

    /** Закрыть филиал — данные и история остаются, на сайте и в записи он пропадает */
    @Transactional
    public void deactivate(Long id) {
        Branch b = find(id);
        if (!b.isActive()) return;
        assertNotLastActive();
        b.setActive(false);
        branchRepository.save(b);
    }

    private void assertNotLastActive() {
        if (branchRepository.findAllByActiveTrueOrderByIdAsc().size() <= 1) {
            throw new RuntimeException("Нельзя закрыть последний работающий филиал");
        }
    }

    /**
     * Назначить руководителей филиала. Клиент или мастер, назначенный руководителем,
     * получает роль ADMIN (карточка мастера при этом сохраняется — он может и дальше принимать).
     */
    @Transactional
    public BranchResponse setManagers(Long id, List<Long> userIds) {
        Branch b = find(id);
        Set<User> managers = new HashSet<>();
        for (Long uid : new LinkedHashSet<>(userIds)) {
            User u = userRepository.findById(uid).orElseThrow(() -> new RuntimeException("Пользователь не найден: " + uid));
            if (u.getRole() == Role.OWNER) continue; // владелец и так видит всё
            if (u.getRole() != Role.ADMIN) {
                u.setRole(Role.ADMIN);
                userRepository.save(u);
            }
            managers.add(u);
        }
        b.getManagers().clear();
        b.getManagers().addAll(managers);
        return BranchResponse.from(branchRepository.save(b));
    }

    /**
     * Задать филиалы мастера. Руководитель меняет только свои филиалы:
     * привязки мастера к чужим филиалам остаются как были.
     */
    @Transactional
    public List<Long> setEmployeeBranches(Long employeeId, List<Long> branchIds, String email) {
        Employee e = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        access.assertCanManage(email, e);

        Set<Long> allowed = access.scope(email, null); // null — владелец, можно любые
        Set<Long> wanted = new HashSet<>(branchIds);

        Set<Branch> result = new HashSet<>();
        for (Branch cur : e.getBranches()) {
            boolean mine = allowed == null || allowed.contains(cur.getId());
            if (!mine || wanted.contains(cur.getId())) result.add(cur);
        }
        for (Long bid : wanted) {
            if (result.stream().anyMatch(b -> b.getId().equals(bid))) continue;
            if (allowed != null && !allowed.contains(bid)) {
                throw new AccessDeniedException("Можно добавлять только в свои филиалы");
            }
            result.add(find(bid));
        }
        e.getBranches().clear();
        e.getBranches().addAll(result);
        employeeRepository.save(e);
        return result.stream().map(Branch::getId).sorted().toList();
    }

    private Branch find(Long id) {
        return branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Филиал не найден: " + id));
    }

    private static void apply(Branch b, Map<String, Object> body) {
        if (body.containsKey("name")) b.setName(str(body.get("name")));
        if (body.containsKey("address")) b.setAddress(str(body.get("address")));
        if (body.containsKey("phone")) b.setPhone(str(body.get("phone")));
        if (body.containsKey("mapUrl")) b.setMapUrl(str(body.get("mapUrl")));
        if (body.containsKey("workingHours")) b.setWorkingHours(str(body.get("workingHours")));
        if (body.containsKey("active")) b.setActive(Boolean.TRUE.equals(body.get("active")));
        if (b.getName() == null) throw new RuntimeException("Укажите название филиала");
    }

    private static String str(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}

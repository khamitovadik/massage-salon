package com.salon.repository;

import com.salon.entity.Appointment;
import com.salon.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByClientIdOrderByStartTimeDesc(Long clientId);
    List<Appointment> findAllByEmployeeIdOrderByStartTimeDesc(Long employeeId);
    List<Appointment> findAllByStatusOrderByStartTimeDesc(AppointmentStatus status);
    List<Appointment> findAllByOrderByStartTimeDesc();

    /** Проверка конфликта времени у сотрудника */
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.employee.id = :employeeId
          AND a.status NOT IN ('CANCELLED')
          AND a.startTime < :endTime
          AND a.endTime > :startTime
    """)
    boolean hasConflict(
        @Param("employeeId") Long employeeId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /** Для напоминаний: записи в интервале времени со статусом PENDING/CONFIRMED */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.startTime >= :from AND a.startTime < :to
          AND a.status IN ('PENDING', 'CONFIRMED')
    """)
    List<Appointment> findUpcoming(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /** 📊 ДЛЯ ГРАФИКА: ВСЕ записи в период времени (для отображения на календаре) */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.startTime >= :from AND a.startTime <= :to
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findAllInRange(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    // === Аналитика ===

    /** Кол-во и выручка по статусам за период */
    @Query("""
        SELECT CAST(a.status AS string), COUNT(a), SUM(a.service.price)
        FROM Appointment a
        WHERE a.startTime >= :from AND a.startTime < :to
        GROUP BY a.status
    """)
    List<Object[]> findAnalyticsRaw(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /** Уникальные клиенты за период */
    @Query("""
        SELECT COUNT(DISTINCT a.client.id) FROM Appointment a
        WHERE a.startTime >= :from AND a.startTime < :to
    """)
    long countUniqueClients(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /** Выручка и кол-во по услугам за период (только COMPLETED) */
    @Query("""
        SELECT a.service.id, a.service.name, COUNT(a), SUM(a.service.price)
        FROM Appointment a
        WHERE a.startTime >= :from AND a.startTime < :to
          AND a.status = 'COMPLETED'
        GROUP BY a.service.id, a.service.name
        ORDER BY COUNT(a) DESC
    """)
    List<Object[]> findRevenueByService(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /** Нагрузка сотрудников за период */
    @Query("""
        SELECT a.employee.id, a.employee.user.name, a.employee.specialization,
               COUNT(a),
               SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END)
        FROM Appointment a
        WHERE a.startTime >= :from AND a.startTime < :to
        GROUP BY a.employee.id, a.employee.user.name, a.employee.specialization
        ORDER BY COUNT(a) DESC
    """)
    List<Object[]> findEmployeeLoad(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /** Записи по дням за период */
    @Query(value = """
        SELECT TO_CHAR(start_time, 'YYYY-MM-DD') AS day,
               COUNT(*) AS cnt,
               SUM(CASE WHEN status = 'COMPLETED' THEN price ELSE 0 END) AS rev
        FROM appointments a
        JOIN services s ON s.id = a.service_id
        WHERE a.start_time >= :from AND a.start_time < :to
        GROUP BY day ORDER BY day
    """, nativeQuery = true)
    List<Object[]> findAppointmentsByDay(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}

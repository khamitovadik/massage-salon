package com.salon.repository;

import com.salon.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByAppointmentId(Long appointmentId);

    void deleteByAppointmentId(Long appointmentId);

    List<Review> findAllByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<Review> findAllByOrderByCreatedAtDesc();

    /** Оценки по сеансам периода: id мастера, оценка */
    @Query("SELECT r.employee.id, r.rating, b.id FROM Review r JOIN r.appointment a LEFT JOIN a.branch b "
         + "WHERE a.startTime >= :from AND a.startTime < :to")
    List<Object[]> findRatingsInRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.employee.id = :employeeId")
    Double getAverageRatingByEmployee(@Param("employeeId") Long employeeId);
}

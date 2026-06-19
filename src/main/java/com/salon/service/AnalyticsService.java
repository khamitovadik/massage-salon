package com.salon.service;

import com.salon.dto.response.AnalyticsResponse;
import com.salon.entity.AppointmentStatus;
import com.salon.entity.SubscriptionStatus;
import com.salon.repository.AppointmentRepository;
import com.salon.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AnalyticsResponse getSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<Object[]> raw = appointmentRepository.findAnalyticsRaw(fromDt, toDt);

        long total = 0, completed = 0, cancelled = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (Object[] row : raw) {
            AppointmentStatus status = AppointmentStatus.valueOf((String) row[0]);
            long cnt = ((Number) row[1]).longValue();
            BigDecimal rev = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            total += cnt;
            if (status == AppointmentStatus.COMPLETED) {
                completed = cnt;
                revenue = rev;
            }
            if (status == AppointmentStatus.CANCELLED) cancelled = cnt;
        }

        long uniqueClients = appointmentRepository.countUniqueClients(fromDt, toDt);
        BigDecimal avgCheck = completed > 0 ? revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<AnalyticsResponse.ServiceStat> byService = appointmentRepository
            .findRevenueByService(fromDt, toDt)
            .stream()
            .map(r -> AnalyticsResponse.ServiceStat.builder()
                .serviceId(((Number) r[0]).longValue())
                .serviceName((String) r[1])
                .count(((Number) r[2]).longValue())
                .revenue(r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO)
                .build())
            .toList();

        List<AnalyticsResponse.EmployeeStat> byEmployee = appointmentRepository
            .findEmployeeLoad(fromDt, toDt)
            .stream()
            .map(r -> AnalyticsResponse.EmployeeStat.builder()
                .employeeId(((Number) r[0]).longValue())
                .employeeName((String) r[1])
                .specialization((String) r[2])
                .totalAppointments(((Number) r[3]).longValue())
                .completedAppointments(((Number) r[4]).longValue())
                .build())
            .toList();

        List<AnalyticsResponse.DayStat> byDay = buildDayStats(from, to, fromDt, toDt);

        long activeSubs = subscriptionRepository.findAllByOrderByCreatedAtDesc()
            .stream().filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE).count();
        long totalSubSessions = subscriptionRepository.findAllByOrderByCreatedAtDesc()
            .stream().filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .mapToLong(s -> s.getRemainingSessions()).sum();

        return AnalyticsResponse.builder()
            .totalRevenue(revenue)
            .totalAppointments(total)
            .completedAppointments(completed)
            .cancelledAppointments(cancelled)
            .uniqueClients(uniqueClients)
            .averageCheck(avgCheck)
            .revenueByService(byService)
            .employeeLoad(byEmployee)
            .appointmentsByDay(byDay)
            .activeSubscriptions(activeSubs)
            .totalSubscriptionSessions(totalSubSessions)
            .build();
    }

    private List<AnalyticsResponse.DayStat> buildDayStats(LocalDate from, LocalDate to,
                                                            LocalDateTime fromDt, LocalDateTime toDt) {
        List<Object[]> raw = appointmentRepository.findAppointmentsByDay(fromDt, toDt);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<AnalyticsResponse.DayStat> result = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            final String dateStr = cur.format(fmt);
            Object[] match = raw.stream()
                .filter(r -> dateStr.equals(r[0]))
                .findFirst().orElse(null);
            result.add(AnalyticsResponse.DayStat.builder()
                .date(dateStr)
                .count(match != null ? ((Number) match[1]).longValue() : 0L)
                .revenue(match != null && match[2] != null ? (BigDecimal) match[2] : BigDecimal.ZERO)
                .build());
            cur = cur.plusDays(1);
        }
        return result;
    }
}

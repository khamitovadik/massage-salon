package com.salon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsResponse {

    // Сводка за период
    private BigDecimal totalRevenue;
    private Long totalAppointments;
    private Long completedAppointments;
    private Long cancelledAppointments;
    private Long uniqueClients;
    private BigDecimal averageCheck;

    // Выручка по услугам
    private List<ServiceStat> revenueByService;

    // Нагрузка сотрудников
    private List<EmployeeStat> employeeLoad;

    // Записи по дням
    private List<DayStat> appointmentsByDay;

    // Активные абонементы
    private Long activeSubscriptions;
    private Long totalSubscriptionSessions;

    @Data
    @Builder
    public static class ServiceStat {
        private Long serviceId;
        private String serviceName;
        private Long count;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class EmployeeStat {
        private Long employeeId;
        private String employeeName;
        private String specialization;
        private Long totalAppointments;
        private Long completedAppointments;
    }

    @Data
    @Builder
    public static class DayStat {
        private String date;
        private Long count;
        private BigDecimal revenue;
    }
}

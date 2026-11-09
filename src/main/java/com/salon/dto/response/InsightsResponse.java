package com.salon.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Расширенная аналитика: сравнение периодов, клиенты, загрузка по времени, отзывы, выводы. */
public record InsightsResponse(
    Kpi current,
    Kpi previous,
    List<Day> byDay,
    /** Загрузка: [день недели 0=Пн..6=Вс][час 0..23] — число неотменённых записей */
    int[][] heatmap,
    List<Weekday> weekdays,
    List<ServiceRow> services,
    List<EmployeeRow> employees,
    Clients clients,
    Reviews reviews,
    Subscriptions subscriptions,
    double avgLeadDays,
    List<Insight> insights
) {
    public record Kpi(BigDecimal revenue, long total, long completed, long cancelled,
                      double cancelRate, BigDecimal avgCheck, long clients) {}

    public record Day(String date, long count, BigDecimal revenue) {}

    public record Weekday(String name, long total, long cancelled) {}

    public record ServiceRow(String name, long count, BigDecimal revenue, double share, long cancelled) {}

    public record EmployeeRow(Long id, String name, String specialization, long total, long completed,
                              long cancelled, BigDecimal revenue, Double avgRating, long reviewCount) {}

    public record Clients(long newClients, long returningClients, double repeatRate,
                          List<ClientRow> top, List<ClientRow> lapsed) {}

    public record ClientRow(String name, String phone, long visits, BigDecimal spent, String lastVisit, long daysSince) {}

    public record Reviews(double average, long count, int[] distribution, double reviewRate) {}

    public record Subscriptions(long active, long expiringSoon, long visitsBySubscription, double subscriptionShare) {}

    /** level: good | warn | info */
    public record Insight(String level, String title, String text) {}
}

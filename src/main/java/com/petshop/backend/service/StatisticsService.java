package com.petshop.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;

    public StatisticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;  // 
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            Map<String, Object> rev = jdbcTemplate.queryForMap("""
                SELECT
                    ISNULL(SUM(p.Amount), 0)                          AS totalRevenue,
                    ISNULL(SUM(CASE WHEN CAST(p.PaymentDate AS DATE) = CAST(GETDATE() AS DATE)
                                    THEN p.Amount ELSE 0 END), 0)     AS todayRevenue,
                    ISNULL(SUM(CASE WHEN YEAR(p.PaymentDate)  = YEAR(GETDATE())
                                     AND MONTH(p.PaymentDate) = MONTH(GETDATE())
                                    THEN p.Amount ELSE 0 END), 0)     AS monthRevenue,
                    COUNT(*)                                           AS totalPayments
                FROM dbo.Payments p
                WHERE p.Status = 'Completed'
            """);
            summary.putAll(rev);
        } catch (Exception e) {
            summary.put("totalRevenue", 0);
            summary.put("todayRevenue", 0);
            summary.put("monthRevenue", 0);
            summary.put("totalPayments", 0);
        }

        try {
            Map<String, Object> ord = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(*)                                                          AS totalOrders,
                    SUM(CASE WHEN Status = 'Pending'   THEN 1 ELSE 0 END)            AS pendingOrders,
                    SUM(CASE WHEN Status = 'Delivered' THEN 1 ELSE 0 END)            AS deliveredOrders,
                    SUM(CASE WHEN CAST(OrderDate AS DATE) = CAST(GETDATE() AS DATE)
                             THEN 1 ELSE 0 END)                                      AS todayOrders
                FROM dbo.Orders
            """);
            summary.putAll(ord);
        } catch (Exception e) {
            summary.put("totalOrders", 0);
            summary.put("pendingOrders", 0);
        }

        try {
            Map<String, Object> cust = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(*)                                                                   AS totalCustomers,
                    SUM(CASE WHEN Tier IN ('GOLD','PLATINUM') THEN 1 ELSE 0 END)              AS goldPlusCustomers
                FROM dbo.Customers
                WHERE IsActive = 1
            """);
            summary.putAll(cust);
        } catch (Exception e) {
            summary.put("totalCustomers", 0);
            summary.put("goldPlusCustomers", 0);
        }

        try {
            Map<String, Object> svc = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS totalServices
                FROM dbo.Services
                WHERE IsActive = 1
            """);
            summary.putAll(svc);
        } catch (Exception e) {
            summary.put("totalServices", 0);
        }

        try {
            List<Map<String, Object>> methods = jdbcTemplate.queryForList("""
                SELECT PaymentMethod, COUNT(*) AS cnt, ISNULL(SUM(Amount),0) AS total
                FROM dbo.Payments
                WHERE Status = 'Completed'
                GROUP BY PaymentMethod
            """);
            summary.put("paymentMethods", methods);
        } catch (Exception e) {
            summary.put("paymentMethods", List.of());
        }

        return summary;
    }

    public List<Map<String, Object>> getDailyRevenue() {
        return jdbcTemplate.queryForList("""
            SELECT orderDate, totalOrders, totalRevenue, uniqueCustomers
            FROM vw_DailyRevenue
            ORDER BY orderDate DESC
        """);
    }

    public List<Map<String, Object>> getMonthlyRevenue() {
        return jdbcTemplate.queryForList("""
            SELECT year, month, totalOrders, totalRevenue, avgOrderValue
            FROM vw_RevenueByMonth
            ORDER BY year DESC, month DESC
        """);
    }

    public List<Map<String, Object>> getTopSellingProducts() {
        return jdbcTemplate.queryForList("""
            SELECT productId, productName, categoryName, price, stockQuantity,
                   totalQuantitySold, totalRevenue, totalOrders
            FROM vw_TopSellingProducts
            ORDER BY totalQuantitySold DESC
        """);
    }

    public List<Map<String, Object>> getRecentOrders() {
        return jdbcTemplate.queryForList("""
            SELECT TOP 10
                o.OrderID, o.OrderDate, o.FinalAmount, o.Status,
                c.FullName AS customerName, c.CustomerCode,
                p.Status AS paymentStatus, p.PaymentMethod
            FROM dbo.Orders o
            LEFT JOIN dbo.Customers c ON c.CustomerID = o.CustomerID
            LEFT JOIN (
                SELECT OrderID, Status, PaymentMethod,
                       ROW_NUMBER() OVER (PARTITION BY OrderID ORDER BY PaymentDate DESC) AS rn
                FROM dbo.Payments WHERE Status = 'Completed'
            ) p ON p.OrderID = o.OrderID AND p.rn = 1
            ORDER BY o.OrderDate DESC
        """);
    }
}
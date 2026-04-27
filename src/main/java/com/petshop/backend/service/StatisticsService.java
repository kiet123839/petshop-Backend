package com.petshop.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;

    public StatisticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getDailyRevenue() {
        String sql = """
            SELECT orderDate, totalOrders, totalRevenue, uniqueCustomers
            FROM vw_DailyRevenue
            ORDER BY orderDate DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMonthlyRevenue() {
        String sql = """
            SELECT year, month, totalOrders, totalRevenue, avgOrderValue
            FROM vw_RevenueByMonth
            ORDER BY year DESC, month DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getTopSellingProducts() {
        String sql = """
            SELECT productId, productName, categoryName, price, stockQuantity,
                   totalQuantitySold, totalRevenue, totalOrders
            FROM vw_TopSellingProducts
            ORDER BY totalQuantitySold DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }
}
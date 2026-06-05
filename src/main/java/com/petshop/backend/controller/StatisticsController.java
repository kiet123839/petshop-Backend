package com.petshop.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petshop.backend.service.StatisticsService;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/revenue/daily")
    public List<Map<String, Object>> getDailyRevenue() {
        return statisticsService.getDailyRevenue();
    }

    @GetMapping("/revenue/monthly")
    public List<Map<String, Object>> getMonthlyRevenue() {
        return statisticsService.getMonthlyRevenue();
    }

    @GetMapping("/top-products")
    public List<Map<String, Object>> getTopSellingProducts() {
        return statisticsService.getTopSellingProducts();
    }

    // ✅ THÊM: endpoint dashboard summary cho các card thống kê
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardSummary() {
        return statisticsService.getDashboardSummary();
    }

    // ✅ THÊM: endpoint 10 đơn hàng gần nhất
    @GetMapping("/recent-orders")
    public List<Map<String, Object>> getRecentOrders() {
        return statisticsService.getRecentOrders();
    }
}
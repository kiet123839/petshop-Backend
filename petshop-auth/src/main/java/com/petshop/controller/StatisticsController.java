package com.petshop.controller;

import com.petshop.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
}
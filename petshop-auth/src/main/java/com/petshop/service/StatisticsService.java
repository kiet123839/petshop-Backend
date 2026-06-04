package com.petshop.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
            SELECT NgayDoanhThu, TongDonHang, TongDoanhThu, KhachHangDuyNhat
            FROM vw_DailyRevenue
            ORDER BY NgayDoanhThu DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMonthlyRevenue() {
        String sql = """
            SELECT NamDoanhThu, ThangDoanhThu, TenThang, TongDonHang, TongDoanhThu, TongGiamGia, GiaTriDonHangTrungBinh
            FROM vw_RevenueByMonth
            ORDER BY NamDoanhThu DESC, ThangDoanhThu DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getTopSellingProducts() {
        String sql = """
            SELECT ProductID, ProductName, CategoryName, Price, StockQuantity, TongSoLuongBan, TongDoanhThu, SoLanXuatHienTrongDon
            FROM vw_TopSellingProducts
            ORDER BY TongSoLuongBan DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }
}
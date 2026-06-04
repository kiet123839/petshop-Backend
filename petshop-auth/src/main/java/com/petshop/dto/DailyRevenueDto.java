package com.petshop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyRevenueDto {
    private LocalDate ngayDoanhThu;
    private Integer tongDonHang;
    private BigDecimal tongDoanhThu;
    private Integer khachHangDuyNhat;

    public DailyRevenueDto(LocalDate ngayDoanhThu, Integer tongDonHang, BigDecimal tongDoanhThu, Integer khachHangDuyNhat) {
        this.ngayDoanhThu = ngayDoanhThu;
        this.tongDonHang = tongDonHang;
        this.tongDoanhThu = tongDoanhThu;
        this.khachHangDuyNhat = khachHangDuyNhat;
    }

    public LocalDate getNgayDoanhThu() {
        return ngayDoanhThu;
    }

    public Integer getTongDonHang() {
        return tongDonHang;
    }

    public BigDecimal getTongDoanhThu() {
        return tongDoanhThu;
    }

    public Integer getKhachHangDuyNhat() {
        return khachHangDuyNhat;
    }
}
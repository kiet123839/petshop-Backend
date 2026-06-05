package com.petshop.backend.repository;

import com.petshop.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy tất cả đơn hàng của một customer
    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    // Lấy đơn hàng theo trạng thái
    List<Order> findByStatusOrderByOrderDateDesc(String status);

    // Lấy tất cả đơn hàng kèm chi tiết (tránh N+1 khi load list)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.product " +
           "ORDER BY o.orderDate DESC")
    List<Order> findAllWithDetails();

    // Lấy đơn hàng kèm chi tiết và thông tin customer (tránh N+1)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.product " +
           "WHERE o.id = :orderId")
    java.util.Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}
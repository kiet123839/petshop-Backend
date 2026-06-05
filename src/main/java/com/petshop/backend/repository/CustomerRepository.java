package com.petshop.backend.repository;

import com.petshop.backend.model.Customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Tìm theo mã khách hàng
    Optional<Customer> findByCustomerCode(String customerCode);

    // Tìm customer theo userId
    Optional<Customer> findByUserId(Long id);

	
}

package com.petshop.backend.repository;

import com.petshop.backend.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    // Đúng cú pháp: truy cập qua object customer rồi lấy id
    List<Pet> findByCustomer_Id(Long customerId);
}

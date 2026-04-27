package com.petshop.backend.service;

import com.petshop.backend.dto.CustomerRequest;
import com.petshop.backend.dto.CustomerResponse;
import com.petshop.backend.model.Customer;
import com.petshop.backend.model.Pet;
import com.petshop.backend.repository.BookingRepository;
import com.petshop.backend.repository.CustomerRepository;
import com.petshop.backend.repository.OrderRepository;
import com.petshop.backend.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PetRepository      petRepository;
    private final BookingRepository  bookingRepository;
    private final OrderRepository    orderRepository;

    public CustomerResponse createCustomer(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setBirthDate(request.getBirthDate());
        customer.setCustomerCode("CUS" + System.currentTimeMillis());
        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy customer id: " + id));
        return toResponse(c);
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy customer id: " + id));
        c.setFullName(request.getFullName());
        c.setPhone(request.getPhone());
        c.setEmail(request.getEmail());
        c.setAddress(request.getAddress());
        c.setBirthDate(request.getBirthDate());
        return toResponse(customerRepository.save(c));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy customer id: " + id));

        // 1. Xóa bookings của từng pet
        List<Pet> pets = petRepository.findByCustomer_Id(id);
        for (Pet pet : pets) {
            bookingRepository.deleteAll(
                bookingRepository.findByPet_IdOrderByBookingDateDesc(pet.getId())
            );
        }

        // 2. Xóa bookings của customer (phòng trường hợp booking không có pet)
        bookingRepository.deleteAll(
            bookingRepository.findByCustomer_IdOrderByBookingDateDesc(id)
        );

        // 3. Xóa orders của customer
        orderRepository.deleteAll(
            orderRepository.findByCustomerIdOrderByOrderDateDesc(id)
        );

        // 4. Xóa pets
        petRepository.deleteAll(pets);

        // 5. Xóa customer
        customerRepository.delete(customer);
    }

    private CustomerResponse toResponse(Customer c) {
        CustomerResponse res = new CustomerResponse();
        res.setId(c.getId());
        res.setCustomerCode(c.getCustomerCode());
        res.setFullName(c.getFullName());
        res.setPhone(c.getPhone());
        res.setEmail(c.getEmail());
        res.setAddress(c.getAddress());
        res.setBirthDate(c.getBirthDate());
        res.setLoyaltyPoints(c.getLoyaltyPoints());
        res.setTier(c.getTier());
        res.setIsActive(c.getIsActive());
        return res;
    }
}
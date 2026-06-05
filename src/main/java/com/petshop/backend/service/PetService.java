package com.petshop.backend.service;

import com.petshop.backend.dto.PetRequest;
import com.petshop.backend.dto.PetResponse;
import com.petshop.backend.model.Customer;
import com.petshop.backend.model.Pet;
import com.petshop.backend.repository.BookingRepository;
import com.petshop.backend.repository.CustomerRepository;
import com.petshop.backend.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public PetService(PetRepository petRepository, CustomerRepository customerRepository, BookingRepository bookingRepository) {
        this.petRepository = petRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }

    public PetResponse createPet(PetRequest request) {
        Customer customer;

        if (request.getCustomerCode() != null && !request.getCustomerCode().isBlank()) {
            customer = findCustomerByCode(request.getCustomerCode());
        } else {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay customer id: " + request.getCustomerId()));
        }

        Pet pet = new Pet();
        pet.setCustomer(customer);
        pet.setName(request.getName());
        pet.setSpecies(request.getSpecies());
        pet.setBreed(request.getBreed());
        pet.setGender(request.getGender());
        pet.setBirthDate(request.getBirthDate());
        pet.setWeight(request.getWeight());
        pet.setColor(request.getColor());
        pet.setHealthNotes(request.getHealthNotes());
        return toResponse(petRepository.save(pet));
    }

    public List<PetResponse> getPetsByCustomerId(Long customerId) {
        return petRepository.findByCustomer_IdAndIsActiveTrue(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PetResponse> getPetsByCustomerCode(String customerCode) {
        Customer customer = findCustomerByCode(customerCode);
        return petRepository.findByCustomer_IdAndIsActiveTrue(customer.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PetResponse getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thu cung id: " + id));
        return toResponse(pet);
    }

    public PetResponse updatePet(Long id, PetRequest request) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thu cung id: " + id));

        if (request.getName() != null) pet.setName(request.getName());
        if (request.getSpecies() != null) pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null) pet.setBreed(request.getBreed());
        if (request.getGender() != null) pet.setGender(request.getGender());
        if (request.getBirthDate() != null) pet.setBirthDate(request.getBirthDate());
        if (request.getWeight() != null) pet.setWeight(request.getWeight());
        if (request.getColor() != null) pet.setColor(request.getColor());
        if (request.getHealthNotes() != null) pet.setHealthNotes(request.getHealthNotes());

        return toResponse(petRepository.save(pet));
    }

    @Transactional
    public void deletePet(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thu cung id: " + id));

        if (!bookingRepository.existsByPet_Id(id)) {
            petRepository.delete(pet);
            return;
        }

        pet.setIsActive(false);
        pet.setUpdatedAt(LocalDateTime.now());
        petRepository.save(pet);
    }

    private PetResponse toResponse(Pet p) {
        PetResponse res = new PetResponse();
        res.setId(p.getId());
        res.setCustomerId(p.getCustomer().getId());
        res.setCustomerCode(formatCustomerCode(p.getCustomer().getId()));
        res.setName(p.getName());
        res.setSpecies(p.getSpecies());
        res.setBreed(p.getBreed());
        res.setGender(p.getGender());
        res.setBirthDate(p.getBirthDate());
        res.setWeight(p.getWeight());
        res.setIsActive(p.getIsActive());
        return res;
    }

    private Customer findCustomerByCode(String customerCode) {
        String code = customerCode.trim().toUpperCase();
        if (code.matches("KH0*\\d+")) {
            Long id = Long.parseLong(code.replaceFirst("^KH0*", ""));
            return customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay ma khach hang: " + customerCode));
        }
        return customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ma khach hang: " + customerCode));
    }

    private String formatCustomerCode(Long id) {
        return "KH" + String.format("%03d", id == null ? 0 : id);
    }
}

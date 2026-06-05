package com.petshop.backend.controller;

import com.petshop.backend.dto.PetRequest;
import com.petshop.backend.dto.PetResponse;
import com.petshop.backend.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    public PetController(PetService petService) {
        this.petService = petService;
    }


    @PostMapping
    public ResponseEntity<PetResponse> createPet(@RequestBody PetRequest request) {
        return new ResponseEntity<>(petService.createPet(request), HttpStatus.CREATED);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PetResponse>> getPetsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(petService.getPetsByCustomerId(customerId));
    }
    @GetMapping("/{id}")
    public ResponseEntity<Object> getPetById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updatePet(@PathVariable Long id,
                                                  @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.updatePet(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
}
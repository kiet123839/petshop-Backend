package com.petshop.backend.service;

import com.petshop.backend.dto.PetRequest;
import com.petshop.backend.dto.PetResponse;
import com.petshop.backend.model.Customer;
import com.petshop.backend.model.Pet;
import com.petshop.backend.repository.CustomerRepository;
import com.petshop.backend.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final CustomerRepository customerRepository;

    public PetResponse createPet(PetRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy customer id: " + request.getCustomerId()));
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
        return petRepository.findByCustomer_Id(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private PetResponse toResponse(Pet p) {
        PetResponse res = new PetResponse();
        res.setId(p.getId());
        res.setCustomerId(p.getCustomer().getId());
        res.setName(p.getName());
        res.setSpecies(p.getSpecies());
        res.setBreed(p.getBreed());
        res.setGender(p.getGender());
        res.setBirthDate(p.getBirthDate());
        res.setWeight(p.getWeight());
        return res;
    }

	public Object getPetById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object updatePet(Long id, PetRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public void deletePet(Long id) {
		// TODO Auto-generated method stub
		
	}
	
}
package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.entity.DiscountPolicy;
import com.tourbooking.booking.backend.repository.DiscountPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discount-policies")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DiscountPolicyController {

    private final DiscountPolicyRepository discountPolicyRepository;

    @GetMapping
    public ResponseEntity<List<DiscountPolicy>> getAllPolicies() {
        return ResponseEntity.ok(discountPolicyRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountPolicy> updatePolicy(@PathVariable Long id, @RequestBody DiscountPolicy request) {
        DiscountPolicy policy = discountPolicyRepository.findById(id).orElseThrow();
        policy.setRate(request.getRate());
        policy.setIsActive(request.getIsActive());
        return ResponseEntity.ok(discountPolicyRepository.save(policy));
    }
}
